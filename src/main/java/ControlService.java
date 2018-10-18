import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 @author veronika K. on 18.10.18 */

@Path("/control")
public class ControlService {

	@GET
	@Path("test")
	@Produces(MediaType.TEXT_PLAIN)
	public String test() {
		return "CONFIG_SCHEMA";
	}

}
