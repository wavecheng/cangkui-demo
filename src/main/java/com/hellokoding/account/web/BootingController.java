package com.hellokoding.account.web;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.RestUrlBuilder;
import org.activiti.rest.service.api.identity.GroupResponse;
import org.activiti.rest.service.api.identity.UserResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootingController {

  @Autowired
  protected RestResponseFactory restResponseFactory;
  
  @Autowired
  protected IdentityService identityService;
  
  @Autowired
  protected RepositoryService repositoryService;
  
  @Autowired
  protected TaskService taskService;
  
  @Autowired
  protected Environment environment;
  
  protected static final Logger LOGGER = LoggerFactory.getLogger(BootingController.class);
  
  @RequestMapping(value="/boot", method = RequestMethod.GET, produces="application/json")
  public BootingEntity getData(HttpServletRequest request) {

    String serverRootUrl = request.getRequestURL().toString();
    serverRootUrl = serverRootUrl.substring(0, serverRootUrl.indexOf("/boot"));
    RestUrlBuilder urlBuidler = RestUrlBuilder.fromCurrentRequest();
    BootingEntity bootResponse = new BootingEntity();
    initUsersData(bootResponse, urlBuidler);
    initGroupsData(bootResponse, urlBuidler);
    initMemberOfData(bootResponse, request.getRemoteUser());
    initProcessDefinitionData(bootResponse);
    String loggedUserId = SecurityContextHolder.getContext().getAuthentication().getName();
    bootResponse.setUserId(loggedUserId);
    return bootResponse;
  }

  protected void initUsersData(BootingEntity bootResponse, RestUrlBuilder urlBuidler) {
    List<UserResponse> users = new ArrayList<UserResponse>();
    List<User> usersList = identityService.createUserQuery().list();
    for (User user : usersList) {
      users.add(restResponseFactory.createUserResponse(user, false, urlBuidler ));
    }
    bootResponse.setUsers(users);
  }

  protected void initGroupsData(BootingEntity bootResponse, RestUrlBuilder urlBuidler) {
    List<GroupResponse> groups = new ArrayList<GroupResponse>();
    List<Group> groupsList = identityService.createGroupQuery().list();
    for (Group group : groupsList) {
      groups.add(restResponseFactory.createGroupResponse(group, urlBuidler));
    }
    bootResponse.setGroups(groups);
  }

  protected void initMemberOfData(BootingEntity bootResponse, String loggedInUser) {
    bootResponse.setUserId(loggedInUser);
    List<String> groups = new ArrayList<String>();
    List<Group> groupsList = identityService.createGroupQuery().groupMember(loggedInUser).list();

    for (Group group : groupsList) {
      groups.add(group.getId());
    }

    bootResponse.setMemberOf(groups);
  }

  protected void initProcessDefinitionData(BootingEntity bootResponse) {
    List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
    List<ProcessDefinitionResponse> responseList = new ArrayList<ProcessDefinitionResponse>();
    for (ProcessDefinition processDefinition : list) {
      responseList.add(restResponseFactory.createProcessDefinitionResponse(processDefinition));
    }
    bootResponse.setProcessDefinitions(responseList);
  }
  
  //init test data
  @PostConstruct
  public void init() {    
   
      LOGGER.info("Initializing demo groups");
      initDemoGroups();
      LOGGER.info("Initializing demo users");
      initDemoUsers();
      LOGGER.info("Initializing demo process definitions");
      initDemoProcessDefinitions();
  }

  protected void initDemoGroups() {
    String[] assignmentGroups = new String[] {"management", "sales", "marketing", "engineering"};
    for (String groupId : assignmentGroups) {
      createGroup(groupId, "assignment");
    }
    
    String[] securityGroups = new String[] {"user", "admin"}; 
    for (String groupId : securityGroups) {
      createGroup(groupId, "security-role");
    }
  }
  
  protected void createGroup(String groupId, String type) {
    if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
      Group newGroup = identityService.newGroup(groupId);
      newGroup.setName(groupId.substring(0, 1).toUpperCase() + groupId.substring(1));
      newGroup.setType(type);
      identityService.saveGroup(newGroup);
    }
  }

  protected void initDemoUsers() {
    createUser("kermit", "Kermit", "The Frog", "kermit", "kermit@activiti.org", null, Arrays.asList(
        "management", "sales", "marketing", "engineering", "user", "admin"), Arrays.asList("birthDate", "10-10-1955", "jobTitle", "Muppet",
        "location", "Hollywoord", "phone", "+123456789", "twitterName", "alfresco", "skype", "activiti_kermit_frog"));

    createUser("gonzo", "Gonzo", "The Great", "gonzo", "gonzo@activiti.org", null,
        Arrays.asList("management", "sales", "marketing", "user"), null);
    createUser("fozzie", "Fozzie", "Bear", "fozzie", "fozzie@activiti.org", null,
        Arrays.asList("marketing", "engineering", "user"), null);
  }
  
  protected void createUser(String userId, String firstName, String lastName, String password, 
          String email, String imageResource, List<String> groups, List<String> userInfo) {
    
    if (identityService.createUserQuery().userId(userId).count() == 0) {
      
      // Following data can already be set by demo setup script
      
      User user = identityService.newUser(userId);
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setPassword(password);
      user.setEmail(email);
      identityService.saveUser(user);
      
      if (groups != null) {
        for (String group : groups) {
          identityService.createMembership(userId, group);
        }
      }
    }
    
    // Following data is not set by demo setup script     
    // image
    if (imageResource != null) {
      byte[] pictureBytes = IoUtil.readInputStream(this.getClass().getClassLoader().getResourceAsStream(imageResource), null);
      Picture picture = new Picture(pictureBytes, "image/jpeg");
      identityService.setUserPicture(userId, picture);
    }
      
    // user info
    if (userInfo != null) {
      for (int i=0; i<userInfo.size(); i+=2) {
        identityService.setUserInfo(userId, userInfo.get(i), userInfo.get(i+1));
      }
    }
    
  }
  
  protected void initDemoProcessDefinitions() {   
    String deploymentName = "Demo processes";
    List<Deployment> deploymentList = repositoryService.createDeploymentQuery().deploymentName(deploymentName).list();

    if (deploymentList == null || deploymentList.size() == 0) {
      repositoryService.createDeployment().name(deploymentName)
          .addClasspathResource("VacationRequest.bpmn20.xml").addClasspathResource("VacationRequest.svg")
          .addClasspathResource("SimpleProcess.bpmn20.xml").addClasspathResource("SimpleProcess.svg")
          .deploy();
    }
  }
  

  protected String randomSentence(String[] words, int length) {
    Random random = new Random();
    StringBuilder strb = new StringBuilder();
    for (int i=0; i<length; i++) {
      strb.append(words[random.nextInt(words.length)]);
      strb.append(" ");
    }
    return strb.toString().trim();
  }
}