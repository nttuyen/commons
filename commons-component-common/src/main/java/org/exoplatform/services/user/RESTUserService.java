/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.user;

import java.util.Date;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



@Path("/state/")
public class RESTUserService implements ResourceContainer{
  private static final Log LOG = ExoLogger.getLogger(RESTUserService.class);
  private final UserStateService userService;
  
  protected static final String ACTIVITY  = "activity";
  protected static final String STATUS    = "status";
  
  public RESTUserService(UserStateService userService) {
    this.userService = userService;
  }
  
    
  @GET
  @Path("/ping/")
  @RolesAllowed("users")
  public Response updateState() {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    userService.ping(userId);
    return Response.ok().build();
  }
  
  @GET
  @Path("/online/")
  @RolesAllowed("users")
  public Response online() throws ParserConfigurationException, JSONException {
    List<UserStateModel> usersOnline = userService.online();  
    JSONArray json = new JSONArray();    
    for(int i=0; i< usersOnline.size(); i++) {
      UserStateModel model = usersOnline.get(i);
      JSONObject object = new JSONObject();      
      object.put("userId", model.getUserId());
      object.put("lastActivity", model.getLastActivity());
      object.put("status", model.getStatus());
      int iDate = (int) (new Date().getTime()/1000);
      int lastActivity = model.getLastActivity();
      if(lastActivity >= (iDate - UserStateService.delay)) {
        object.put("activity", "online");
      } else {
        object.put("activity", "offline");
      }
      json.put(object);
    }
    return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
  }
  
  @GET
  @Path("/status/{userId}/")
  @RolesAllowed("users")
  public Response getStatus(@PathParam("userId") String userId) throws JSONException {
    UserStateModel model = userService.getUserState(userId);
    JSONObject object = new JSONObject();
    object.put("userId", model.getUserId());
    object.put("lastActivity", model.getLastActivity());
    object.put("status", model.getStatus());
    int iDate = (int) (new Date().getTime()/1000);
    int lastActivity = model.getLastActivity();
    if(lastActivity >= (iDate - UserStateService.delay)) {
      object.put("activity", "online");
    } else {
      object.put("activity", "offline");
    }   
    return Response.ok(object.toString(), MediaType.APPLICATION_JSON).build();
  }
  
  @PUT
  @Path("/status/{userId}/")
  @RolesAllowed("users")
  public Response setStatus(@PathParam("userId") String userId, @QueryParam("status") String status) throws JSONException {
    UserStateModel model = userService.getUserState(userId);
    if(StringUtils.isNotEmpty(status)) {
      model.setStatus(status);
      userService.save(model); 
      return Response.ok().build();
    }
    return Response.notModified().build();
  }
  
  
}
