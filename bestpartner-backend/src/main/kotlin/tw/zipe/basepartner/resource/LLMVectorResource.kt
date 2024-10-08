package tw.zipe.basepartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestQuery

/**
 * @author zipe1
 * @created 2024/10/8
 */
@Path("/llm/vector")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMVectorResource {
//
//    @POST
//    @Path("/storeDocFiles")
//    @Consumes(MediaType.APPLICATION_JSON)
//    fun storeDocFiles(@RestQuery fileDirPath: String, @RestQuery company: Company): String {
//
//    }
}
