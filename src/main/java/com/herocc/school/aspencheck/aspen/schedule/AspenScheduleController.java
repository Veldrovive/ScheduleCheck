package com.herocc.school.aspencheck.aspen.schedule;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.JSONReturn;
import com.herocc.school.aspencheck.ErrorInfo;
import com.herocc.school.aspencheck.aspen.AspenRestController;
import com.herocc.school.aspencheck.aspen.AspenWebFetch;
import org.jsoup.Connection;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.logging.Level;

@CrossOrigin
@RestController
@RequestMapping("aspen")
public class AspenScheduleController extends AspenRestController {
  private Schedule schedule;
  
  @RequestMapping("schedule")
  public JSONReturn restScheduleHandler(@RequestHeader(value="ASPEN_UNAME", required=false) String u,
                                                      @RequestHeader(value="ASPEN_PASS", required=false) String p){
    
    if (u != null && p != null) return new JSONReturn(new ResponseEntity<>(getSchedule(u, p), HttpStatus.OK), new ErrorInfo());
    
    if (AspenCheck.getUnixTime() > getNextRefreshTime()) {
      AspenCheck.log.log(Level.INFO, "Refreshing Aspen Schedule, " + String.valueOf(AspenCheck.getUnixTime() + " > " + getNextRefreshTime()));
      new Thread(this::refresh).start();
    }
    return new JSONReturn(new ResponseEntity<>(schedule, HttpStatus.OK), new ErrorInfo());
  }
  
  @Cacheable("publicSchedule")
  @CacheEvict(value = "publicSchedule", allEntries=true)
  public Schedule refreshSchedule() {
    schedule = getSchedule(AspenCheck.username, AspenCheck.password);
    lastRefreshTimestamp = System.currentTimeMillis() / 1000;
    return schedule;
  }
  
  public Schedule getSchedule(String username, String password) {
    AspenWebFetch aspenWebFetch = new AspenWebFetch(username, password);
    Connection.Response schedulePage = aspenWebFetch.getSchedulePage();
    if (schedulePage != null) {
      try {
        return schedule = new Schedule(schedulePage.parse());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
  
  @Override
  protected void refresh() {
    refreshSchedule();
  }
}
