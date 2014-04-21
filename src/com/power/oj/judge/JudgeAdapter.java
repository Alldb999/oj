package com.power.oj.judge;

import java.io.File;
import java.io.IOException;

import jodd.io.FileUtil;

import com.jfinal.log.Logger;
import com.power.oj.contest.ContestService;
import com.power.oj.contest.model.ContestSolutionModel;
import com.power.oj.core.OjConfig;
import com.power.oj.core.OjConstants;
import com.power.oj.core.bean.ResultType;
import com.power.oj.core.bean.Solution;
import com.power.oj.core.model.ProgramLanguageModel;
import com.power.oj.problem.ProblemService;
import com.power.oj.solution.SolutionModel;
import com.power.oj.user.UserService;

public abstract class JudgeAdapter implements Runnable
{

  protected static final JudgeService judgeService = JudgeService.me();
  protected static final ContestService contestService = ContestService.me();
  protected static final UserService userService = UserService.me();
  protected static final ProblemService problemService = ProblemService.me();

  protected final Logger log = Logger.getLogger(getClass());
  private final ThreadLocal<Solution> solutionLocal = new ThreadLocal<Solution>();
  protected Solution solution;
  
  public JudgeAdapter()
  {
    super();
  }

  public JudgeAdapter(Solution solution)
  {
    this();
    solutionLocal.set(solution);
    this.solution = solution;
  }

  protected abstract boolean Compile() throws IOException;

  protected abstract boolean RunProcess() throws IOException, InterruptedException;

  @Override
  public void run()
  {
    synchronized (JudgeAdapter.class)
    {
      try
      {
        prepare();
        if (Compile())
        {
          RunProcess();
        } else
        {
          log.warn("Compile failed.");
        }
      } catch (Exception e)
      {
        updateSystemError(e.getLocalizedMessage());
  
        if (OjConfig.getDevMode())
          e.printStackTrace();
        log.error(e.getLocalizedMessage());
      }
    }
  }

  protected void prepare() throws IOException
  {
    ProgramLanguageModel programLanguage = OjConfig.language_type.get(solution.getLanguage());
    
    String workPath = judgeService.getWorkPath(solution);
    if (solution instanceof SolutionModel && OjConfig.getBoolean("deleteTmpFile", false))
    {
      File prevWorkDir = new File(new StringBuilder(2).append(workPath).append(solution.getSid() - 5).toString());
      if (prevWorkDir.isDirectory())
      {
        FileUtil.deleteDir(prevWorkDir);
        log.info("Delete previous work directory " + prevWorkDir.getAbsolutePath());
      }
    }

    File workDir = new File(new StringBuilder(2).append(workPath).append(solution.getSid()).toString());
    if (workDir.isDirectory())
    {
      FileUtil.cleanDir(workDir);
    } else
    {
      FileUtil.mkdirs(workDir);
    }
    String workDirPath = workDir.getAbsolutePath();

    File sourceFile = new File(new StringBuilder(5).append(workDirPath).append(File.separator).append(OjConstants.SOURCE_FILE_NAME).append(".")
        .append(programLanguage.getExt()).toString());
    FileUtil.touch(sourceFile);
    FileUtil.writeString(sourceFile, solution.getSource());
  }

  protected boolean updateCompileError(String error)
  {
    solution.setResult(ResultType.CE).setError(error);

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean updateRuntimeError(String error)
  {
    solution.setResult(ResultType.RE).setError(error);

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean updateSystemError(String error)
  {
    solution.setResult(ResultType.SE).setSystemError(error);

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean updateResult(int result, int time, int memory)
  {
    solution.setResult(result).setTime(time).setMemory(memory);

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean updateResult(boolean ac, Integer test, Integer totalRunTime)
  {
    if (ac)
    {
      solution.setResult(ResultType.AC);
      solution.setTime(totalRunTime);
    } else if (solution.getResult() != ResultType.CE && solution.getResult() != ResultType.RF)
    {
      solution.setTest(test);
    }

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean setResult(int result, int time, int memory)
  {
    boolean needUpdate = (result != solution.getResult());
    solution.setResult(result);
    if (solution.getTime() == null)
    {
      solution.setTime(time);
    } else
    {
      solution.setTime(Math.max(time, solution.getTime()));
    }
    if (solution.getMemory() == null)
    {
      solution.setMemory(memory);
    } else
    {
      solution.setMemory(Math.max(memory, solution.getMemory()));
    }

    if (!needUpdate)
    {
      return true;
    }

    if (solution instanceof ContestSolutionModel)
    {
      return ((ContestSolutionModel) solution).update();
    }
    return ((SolutionModel) solution).update();
  }

  protected boolean updateUser()
  {
    if (solution.getResult() != ResultType.AC)
    {
      return false;
    }

    Integer cid = solution.getCid();
    if (cid != null && cid > 0)
    {
      return false;
    }
    return userService.incAccepted((SolutionModel) solution);
  }

  protected boolean updateProblem()
  {
    if (solution.getResult() != ResultType.AC)
    {
      return false;
    }

    return problemService.incAccepted((SolutionModel) solution);
  }

  protected boolean updateContest()
  {
    if (solution instanceof ContestSolutionModel)
    {
      if (((ContestSolutionModel) solution).get("originalResult") != null)
      {
        contestService.updateBoard4Rejudge((ContestSolutionModel) solution);
      } else
      {
        contestService.updateBoard((ContestSolutionModel) solution);
      }
      return true;
    }
    return false;
  }

}
