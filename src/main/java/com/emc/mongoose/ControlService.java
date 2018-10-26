package com.emc.mongoose;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 @author veronika K. on 25.10.18 */
@Path("/control")
public class ControlService {

	@GET
	public String get() {
		return "OK";
	}
}
