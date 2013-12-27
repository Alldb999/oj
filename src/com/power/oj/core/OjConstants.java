package com.power.oj.core;

/**
 * Some constant objects.
 * 
 * @author power
 * 
 */
public abstract class OjConstants
{
  /*
   * These contants are used in general.
   */
  public final static int SessionExpiresTime = 15 * 60 * 1000;
  
  /*
   * These contants are used in view(attr name) and session.
   */

  public final static String SITE_TITLE = "siteTitle";
  public final static String PAGE_TITLE = "pageTitle";
  public final static String BASE_URL = "baseUrl";
  
  public final static String CONTROLLER_KEY = "controllerKey";
  public final static String ACTION_KEY = "actionKey";
  public final static String METHOD_NAME = "methodName";

  public final static String MSG = "msg";
  public final static String MSG_TYPE = "msgType";
  public final static String MSG_TITLE = "msgTitle";

  public final static String USER = "user";
  public final static String USER_ID = "userID";
  public final static String USER_NAME = "userName";
  public final static String USER_EMAIL = "userEmail";
  public final static String USER_LIST = "userList";
  public final static String ADMIN_USER = "adminUser";

  public final static String PROGRAM_LANGUAGES = "program_languages";
  public final static String LANGUAGE_NAME = "language_name";
  public final static String JUDGE_RESULT = "judge_result";
  public final static String RESULT_TYPE = "result_type";
  
  public final static String LAST_ACCESS_URL = "lastAccessURL";
  /*
   * These contants are used in cookie.
   */
  public final static String randomCodeKey = "PowerOJCaptcha";
  
  public final static int TOKEN_AGE = 3600 * 24 * 7;
  public final static String TOKEN_NAME = "PowerOjName";
  public final static String TOKEN_TOKEN = "PowerOjToken";

}
