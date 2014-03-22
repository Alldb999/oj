package com.power.oj.contest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jodd.util.StringUtil;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.jfinal.aop.Before;
import com.jfinal.aop.ClearInterceptor;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.plugin.activerecord.Page;
import com.power.oj.contest.model.ContestModel;
import com.power.oj.contest.model.ContestSolutionModel;
import com.power.oj.core.OjConfig;
import com.power.oj.core.OjConstants;
import com.power.oj.core.OjController;
import com.power.oj.core.bean.FlashMessage;
import com.power.oj.core.bean.MessageType;
import com.power.oj.core.bean.ResultType;
import com.power.oj.core.model.ProgramLanguageModel;
import com.power.oj.core.service.SessionService;
import com.power.oj.problem.ProblemModel;
import com.power.oj.solution.SolutionService;
import com.power.oj.user.UserService;
import com.power.oj.util.CryptUtils;

@Before({ContestPasswordInterceptor.class, ContestInterceptor.class})
public class ContestController extends OjController
{
  private static final SolutionService solutionService = SolutionService.me();
  private static final UserService userService = UserService.me();
  private static final ContestService contestService = ContestService.me();

  @ClearInterceptor
  public void index()
  {
    int pageNumber = getParaToInt(0, 1);
    int pageSize = getParaToInt(1, OjConfig.contestPageSize);
    Integer type = getParaToInt("type", -1);
    Integer status = getParaToInt("status", -1);

    setAttr("contestList", contestService.getContestList(pageNumber, pageSize, type, status));
    setAttr("pageSize", OjConfig.contestPageSize);

    setTitle(getText("contest.index.title"));
  }

  public void show()
  {
    Integer cid = getParaToInt(0);
    Integer uid = userService.getCurrentUid();
    
    ContestModel contestModel = contestService.getContest(cid);
    
    long serverTime = OjConfig.timeStamp;
    int startTime = contestModel.getStartTime();
    int endTime = contestModel.getEndTime();
    String status = getText("contest.status.running");

    if (startTime > serverTime)
      status = getText("contest.status.pending");
    else if (endTime < serverTime)
      status = getText("contest.status.finished");

    setAttr("contest", contestModel);
    setAttr("contestProblems", contestService.getContestProblems(cid, uid));
    setAttr("status", status);

    setTitle(new StringBuilder(2).append(getText("contest.show.title")).append(cid).toString());
  }

  public void problem()
  {
    Integer cid = getParaToInt(0);
    String problem_id = getPara(1);
    if (problem_id == null)
    {
      forwardAction("/contest/allProblems/" + cid);
      return;
    }
    char id = problem_id.toUpperCase().charAt(0);
    Integer num = id - 'A';

    ProblemModel problemModel = contestService.getProblem(cid, num);
    if (problemModel == null)
    {
      FlashMessage msg = new FlashMessage(getText("contest.problem.null"), MessageType.ERROR, getText("message.error.title"));
      redirect(new StringBuilder(2).append("/contest/show/").append(cid).toString(), msg);
      return;
    }

    setAttr("problem", problemModel);
    setAttr("userResult", contestService.getUserResult(cid, num));
    setAttr("cstatus", contestService.getContestStatus(cid));
    setAttr("contestProblems", contestService.getContestProblems(cid, null));

    setTitle(new StringBuilder(5).append(cid).append("-").append(id).append(": ").append(problemModel.getTitle()).toString());
  }
  
  public void allProblems()
  {
    Integer cid = getParaToInt(0);
    setAttr("contestProblems", contestService.getContestProblems(cid));
  }

  public void submit()
  {
    Integer cid = getParaToInt(0);
    String problem_id = getPara(1, "A");
    char id = problem_id.toUpperCase().charAt(0);
    Integer num = id - 'A';
    boolean ajax = getParaToBoolean("ajax", false);

    if (contestService.isContestFinished(cid))
    {
      FlashMessage msg = new FlashMessage(getText("contest.submit.finished"), MessageType.WARN, getText("message.warn.title"));
      redirect(new StringBuilder(2).append("/contest/show/").append(cid).toString(), msg);
      return;
    }

    ProblemModel problemModel = contestService.getProblem(cid, num);
    if (problemModel == null)
    {
      FlashMessage msg = new FlashMessage(getText("contest.problem.null"), MessageType.ERROR, getText("message.error.title"));
      redirect(new StringBuilder(2).append("/contest/show/").append(cid).toString(), msg);
      return;
    }

    setAttr("problem", problemModel);
    setAttr(OjConstants.PROGRAM_LANGUAGES, OjConfig.program_languages);
    
    setTitle(new StringBuilder(6).append(getText("contest.problem.title")).append(cid).append("-").append(id).append(": ").append(problemModel.getTitle()).toString());
    if (ajax)
      render("ajax/submit.html");
    else
      render("submit.html");
  }
  
  @ClearInterceptor
  @Before(POST.class)
  public void submitSolution()
  {
    ContestSolutionModel contestSolution = getModel(ContestSolutionModel.class, "solution");
    Integer cid = contestSolution.getCid();
    String url = new StringBuilder(2).append("/contest/status/").append(cid).toString();
    int result = contestService.submitSolution(contestSolution);
    
    if (result == -1)
    {
      setFlashMessage(new FlashMessage(getText("solution.save.null"), MessageType.ERROR, getText("message.error.title")));
    }
    else if (result == -2)
    {
      setFlashMessage(new FlashMessage(getText("solution.save.error"), MessageType.ERROR, getText("message.error.title")));
    }
    
    redirect(url);
  }

  public void rank()
  {
    Integer cid = getParaToInt(0);
    int pageNumber = getParaToInt(1, 1);
    int pageSize = getParaToInt(2, OjConfig.contestRankPageSize);
    
    setAttr("pageSize", OjConfig.contestRankPageSize);
    setAttr("contestRank", contestService.getContestRank(pageNumber, pageSize, cid));
    setAttr("contestProblems", contestService.getContestProblems(cid, 0));
    setAttr("cstatus", contestService.getContestStatus(cid));

    setTitle(new StringBuilder(2).append(getText("contest.rank.title")).append(cid).toString());
  }

  public void status()
  {
    Integer cid = getParaToInt(0);
    int pageNumber = getParaToInt(1, 1);
    int pageSize = getParaToInt(2, OjConfig.statusPageSize);
    Integer result = getParaToInt("result", -1);
    Integer language = getParaToInt("language", -1);
    Integer num = -1;

    if (StringUtil.isNotBlank(getPara("pid")))
    {
      try
      {
        num = getParaToInt("pid");
      } catch (Exception e)
      {
        num = getPara("pid").toUpperCase().charAt(0) - 'A';
      }
    }
    String userName = getPara("name");
    StringBuilder query = new StringBuilder().append("?cid=").append(cid);

    if (result > -1)
    {
      query.append("&result=").append(result);
    }
    if (language > -1)
    {
      query.append("&language=").append(language);
    }
    if (num > -1)
    {
      query.append("&pid=").append(getPara("pid"));
    }
    if (StringUtil.isNotBlank(userName))
    {
      query.append("&name=").append(userName);
    }

    //setAttr("contest", contestService.getContestById(cid));
    setAttr("contestProblems", contestService.getContestProblems(cid, 0));
    setAttr("solutionList", solutionService.getPageForContest(pageNumber, pageSize, result, language, cid, num, userName));
    setAttr(OjConstants.PROGRAM_LANGUAGES, OjConfig.program_languages);
    setAttr(OjConstants.JUDGE_RESULT, OjConfig.judge_result);
    setAttr("result", result);
    setAttr("language", language);
    setAttr("pid", getPara("pid"));
    setAttr("name", userName);
    setAttr("query", query.toString());

    setTitle(new StringBuilder(2).append(String.format(getText("contest.status.title"),cid)).toString());
  }

  public void problemStatus()
  {
    Integer cid = getParaToInt(0);
    char id = getPara(1, "A").toUpperCase().charAt(0);
    Integer num = id - 'A';
    ProblemModel problemModel = contestService.getProblem(cid, num);
    boolean ajax = getParaToBoolean("ajax", false);

    if (!ajax)
    {
      if (problemModel == null)
      {
        FlashMessage msg = new FlashMessage(getText("contest.problem.null"), MessageType.ERROR, getText("message.error.title"));
        redirect(new StringBuilder(2).append("/contest/show/").append(cid).toString(), msg);
        return;
      }

      setAttr("resultList", solutionService.getProblemStatusForContest(cid, num));
      setAttr("problem", problemModel);
    }

    int pageNumber = getParaToInt(2, 1);
    int pageSize = getParaToInt(3, OjConfig.statusPageSize);
    Integer language = getParaToInt("language", -1);
    StringBuilder query = new StringBuilder();
    if (language > -1)
    {
      query.append("language=").append(language);
    }
    Page<ContestSolutionModel> solutionList = solutionService.getProblemStatusPageForContest(pageNumber, pageSize, language, cid, num);

    setAttr(OjConstants.PROGRAM_LANGUAGES, OjConfig.program_languages);
    setAttr("solutionList", solutionList);
    setAttr("language", language);
    setAttr("query", query.toString());
    setAttr("pageSize", OjConfig.statusPageSize);
    setAttr("id", id);

    setTitle(new StringBuilder(2).append(String.format(getText("contest.status.title"), cid, id)).toString());
    render(ajax ? "ajax/problemStatus.html" : "problemStatus.html");
  }
  
  public void code()
  {
    Integer cid = getParaToInt(0);
    Integer sid = getParaToInt(1);
    boolean isAdmin = userService.isAdmin();
    ContestSolutionModel solutionModel = solutionService.findContestSolution(sid);
    ResultType resultType = (ResultType) OjConfig.result_type.get(solutionModel.getResult());
    int uid = solutionModel.getUid();
    int loginUid = userService.getCurrentUid();
    if (uid != loginUid && !isAdmin)
    {
      FlashMessage msg = new FlashMessage(getText("solution.show.error"), MessageType.ERROR, getText("message.error.title"));
      redirect("/status", msg);
      return;
    }

    if (!isAdmin)
    {
      String error = solutionModel.getError();
      if (error != null)
      {
        solutionModel.set("error", error.replaceAll(StringUtil.replace(OjConfig.get("workPath"), "\\", "\\\\"), ""));
        // TODO replace "/"
      }
    }

    String problemTitle = "";
    int num = solutionModel.getInt("num");
    problemTitle = ContestService.me().getProblemTitle(cid, num);
    setAttr("alpha", (char) (num + 'A'));
    setAttr("cid", cid);

    setAttr("problemTitle", problemTitle);
    try
    {
      setAttr("submitUser", UserService.me().getUserByUid(uid));
    } catch (NullPointerException e)
    {
      log.warn(e.getLocalizedMessage());
    }
    ProgramLanguageModel language = (ProgramLanguageModel) OjConfig.language_type.get(solutionModel.getLanguage());
    setAttr("language", language.getName());

    setAttr("resultLongName", resultType.getLongName());
    setAttr("resultName", resultType.getName());
    setAttr("solution", solutionModel);

    String brush = language.getName().toLowerCase();
    if ("G++".equalsIgnoreCase(brush) || "GCC".equalsIgnoreCase(brush))
    {
      brush = "cpp";
    }
    setAttr("brush", brush);

    setTitle(getText("solution.show.title"));
  }

  public void statistics()
  {
    Integer cid = getParaToInt(0);
    List<String> resultName = new ArrayList<String>();
    
    for (ResultType resultType : OjConfig.judge_result)
    {
      if (resultType.getId() > 9)
        break;
      resultName.add(resultType.getName());
    }
    resultName.add("Others");
    
    setAttr("resultName", resultName);
    setAttr("languageList", OjConfig.program_languages);
    setAttr("statistics", contestService.getContestStatistics(cid));

    setTitle(new StringBuilder(3).append(getText("contest.statistics.title")).append(cid).toString());
  }
  
  public void clarify()
  {
    Integer cid = getParaToInt(0);
    Integer uid = userService.getCurrentUid();
    
    if (userService.isAdmin())
    {
      setAttr("clarifyList", contestService.getClarifyList(cid));
      render("adminClarify.html");
    }
    else
    {
      setAttr("privateClarifyList", contestService.getPrivateClarifyList(cid, uid));
      setAttr("publicClarifyList", contestService.getPublicClarifyList(cid));
      render("clarify.html");
    }
  }
  
  public void report()
  {
    
  }

  @ClearInterceptor
  public void recent()
  {
    setTitle(getText("contest.recent.title"));
  }

  @ClearInterceptor
  @Before(POST.class)
  public void password()
  {
    Integer cid = getParaToInt("cid");
    String password = getPara("password");
    
    if (contestService.checkContestPassword(cid, password))
    {
      String token_name = new StringBuilder("cid-").append(cid).toString();
      String token_token = CryptUtils.encrypt(password, token_name);
      setSessionAttr(token_name, token_token);
      redirect(SessionService.me().getLastAccessURL());
      return;
    }

    keepPara("cid");
    keepPara("title");
    
    FlashMessage msg = new FlashMessage(getText("contest.password.error"), MessageType.ERROR, getText("message.error.title"));
    redirect(SessionService.me().getLastAccessURL(), msg);
  }

  @RequiresPermissions("contest:edit")
  public void edit()
  {
    boolean ajax = getParaToBoolean("ajax", false);
    
    setTitle(getText("contest.edit.title"));
    render(ajax ? "ajax/edit.html" : "edit.html");
  }

  @ClearInterceptor
  @Before(POST.class)
  @RequiresPermissions("contest:edit")
  public void update()
  {
    String startTime = getPara("startTime");
    String endTime = getPara("endTime");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    ContestModel contestModel = getModel(ContestModel.class, "contest");
    
    try
    {
      contestModel.setStartTime((int) (sdf.parse(startTime).getTime() / 1000));
      contestModel.setEndTime((int) (sdf.parse(endTime).getTime() / 1000));
    } catch (ParseException e)
    {
      if (OjConfig.getDevMode())
        e.printStackTrace();
    }
    
    contestService.updateContest(contestModel);
    redirect(new StringBuilder(2).append("/contest/show/").append(contestModel.getInt("cid")).toString());
  }

  @ClearInterceptor
  @RequiresPermissions("contest:add")
  public void add()
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    long ctime = OjConfig.startInterceptorTime + 3600000;
    setAttr("startTime", sdf.format(new Date(ctime)));
    setAttr("endTime", sdf.format(new Date(ctime + 18000000)));
    
    setTitle(getText("contest.add.title"));
  }

  @ClearInterceptor
  @Before(POST.class)
  @RequiresPermissions("contest:add")
  public void save()
  {
    String startTime = getPara("startTime");
    String endTime = getPara("endTime");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    ContestModel contestModel = getModel(ContestModel.class, "contest");
    contestModel.setUid(userService.getCurrentUid());
    try
    {
      contestModel.setStartTime((int) (sdf.parse(startTime).getTime() / 1000));
      contestModel.setEndTime((int) (sdf.parse(endTime).getTime() / 1000));
      log.info(contestModel.getEndTime().toString());
    } catch (ParseException e)
    {
      if (OjConfig.getDevMode())
        e.printStackTrace();
      log.error(e.getLocalizedMessage());
    }
    contestModel.saveContest();

    redirect(new StringBuilder(2).append("/contest/admin/").append(contestModel.getInt("cid")).toString());
  }
  
  @RequiresPermissions("contest:addProblem")
  public void admin()
  {
    Integer cid = getParaToInt(0);
    
    ContestModel contestModel = contestService.getContest(cid);
    
    long serverTime = OjConfig.timeStamp;
    int startTime = contestModel.getInt("startTime");
    int endTime = contestModel.getInt("endTime");
    String status = getText("contest.status.running");

    if (startTime > serverTime)
      status = getText("contest.status.pending");
    else if (endTime < serverTime)
      status = getText("contest.status.finished");

    setAttr("contest", contestModel);
    setAttr("contestProblems", contestService.getContestProblems(cid, null));
    setAttr("status", status);

    //setTitle(new StringBuilder(2).append(getText("contest.admin.title")).append(cid).toString());
  }
  
  @RequiresPermissions("contest:addUser")
  public void adminUser()
  {
    Integer cid = getParaToInt(0);
    
    ContestModel contestModel = contestService.getContest(cid);
    
    long serverTime = OjConfig.timeStamp;
    int startTime = contestModel.getInt("startTime");
    int endTime = contestModel.getInt("endTime");
    String status = getText("contest.status.running");

    if (startTime > serverTime)
      status = getText("contest.status.pending");
    else if (endTime < serverTime)
      status = getText("contest.status.finished");

    setAttr("contest", contestModel);
    setAttr("contestUsers", contestService.getContestUsers(cid));
    setAttr("status", status);

    //setTitle(new StringBuilder(2).append(getText("contest.admin.title")).append(cid).toString());
  }

  @ClearInterceptor
  @RequiresPermissions("contest:build")
  public void buildRank()
  {
    Integer cid = getParaToInt(0);
    contestService.buildRank(cid);

    redirect(new StringBuilder(2).append("/contest/rank/").append(cid).toString(), new FlashMessage(getText("contest.buildRank.success")));
  }

}
