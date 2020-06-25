package org.carlspring.strongbox.gremlin.adapters;

import static org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single;
import static org.carlspring.strongbox.gremlin.dsl.EntityTraversalUtils.extracPropertytList;
import static org.carlspring.strongbox.gremlin.dsl.EntityTraversalUtils.extractObject;
import static org.carlspring.strongbox.gremlin.dsl.EntityTraversalUtils.toLocalDateTime;
import static org.carlspring.strongbox.gremlin.dsl.EntityTraversalUtils.toLong;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.carlspring.strongbox.db.schema.Edges;
import org.carlspring.strongbox.db.schema.Vertices;
import org.carlspring.strongbox.domain.User;
import org.carlspring.strongbox.domain.UserEntity;
import org.carlspring.strongbox.domain.UserRole;
import org.carlspring.strongbox.gremlin.dsl.EntityTraversal;
import org.carlspring.strongbox.gremlin.dsl.EntityTraversalUtils;
import org.carlspring.strongbox.gremlin.dsl.__;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

/**
 * @author sbespalov
 */
@Component
public class UserAdapter implements VertexEntityTraversalAdapter<User>
{

    @Inject
    private UserRoleAdapter userRoleAdapter;
    
    @Override
    public String label()
    {
        return Vertices.USER;
    }

    @Override
    public EntityTraversal<Vertex, User> fold()
    {
        return __.<Vertex, Object>project("id",
                                          "uuid",
                                          "password",
                                          "enabled",
                                          "roles",
                                          "securityTokenKey",
                                          "lastUpdated",
                                          "sourceId")
                 .by(__.id())
                 .by(__.enrichPropertyValue("uuid"))
                 .by(__.enrichPropertyValue("password"))
                 .by(__.enrichPropertyValue("enabled"))
                 .by(__.outE(Edges.USER_HAS_USER_ROLES)
                       .inV()
                       .map(userRoleAdapter.fold())
                       .map(EntityTraversalUtils::castToObject)
                       .fold())
                 .by(__.enrichPropertyValue("securityTokenKey"))
                 .by(__.enrichPropertyValue("lastUpdated"))
                 .by(__.enrichPropertyValue("sourceId"))
                 .map(this::map);
    }

    private User map(Traverser<Map<String, Object>> t)
    {
        UserEntity result = new UserEntity(extractObject(String.class, t.get().get("uuid")));
        result.setNativeId(extractObject(Long.class, t.get().get("id")));

        result.setPassword(extractObject(String.class, t.get().get("password")));
        result.setEnabled(extractObject(Boolean.class, t.get().get("enabled")));
        List<UserRole> userRoles = (List<UserRole>) t.get().get("roles");
        result.setRoles(new HashSet<>(userRoles));
        result.setSecurityTokenKey(extractObject(String.class, t.get().get("securityTokenKey")));
        result.setLastUpdated(toLocalDateTime(extractObject(Long.class, t.get().get("lastUpdated"))));
        result.setSourceId(extractObject(String.class, t.get().get("sourceId")));

        return result;
    }

    @Override
    public UnfoldEntityTraversal<Vertex, Vertex> unfold(User entity)
    {
        EntityTraversal<Vertex, Vertex> t = __.<Vertex>identity();

        if (entity.getPassword() != null)
        {
            t = t.property(single, "password", entity.getPassword());
        }
        if (entity.getSecurityTokenKey() != null)
        {
            t = t.property(single, "securityTokenKey", entity.getSecurityTokenKey());
        }
        if (entity.getSourceId() != null)
        {
            t = t.property(single, "sourceId", entity.getSourceId());
        }
        if (entity.getLastUpdated() != null)
        {
            t = t.property(single, "lastUpdated", toLong(entity.getLastUpdated()));
        }

        t = t.property(single, "enabled", entity.isEnabled());

        return new UnfoldEntityTraversal<>(Vertices.USER, entity, t);
    }

    @Override
    public EntityTraversal<Vertex, Element> cascade()
    {
        return __.<Vertex>identity().map(t -> Element.class.cast(t.get()));
    }

}
