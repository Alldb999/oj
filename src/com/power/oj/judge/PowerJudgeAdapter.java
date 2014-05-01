package com.power.oj.judge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jodd.format.Printf;
import jodd.io.FileNameUtil;
import jodd.util.StringUtil;

import com.power.oj.contest.model.ContestSolutionModel;
import com.power.oj.core.OjConfig;
import com.power.oj.core.bean.ResultType;
import com.power.oj.core.bean.Solution;
import com.power.oj.problem.ProblemModel;

public class PowerJudgeAdapter extends JudgeAdapter
{

  public PowerJudgeAdapter()
  {
    super();
  }
  
  public PowerJudgeAdapter(Solution solution)
  {
    super(solution);
  }
  
  @Override
  protected boolean Compile() throws IOException
  {
    return true;
  }

  @Override
  protected boolean RunProcess() throws IOException, InterruptedException
  {
    ProblemModel problemModel;
    if (solution instanceof ContestSolutionModel)
    {
      problemModel = problemService.findProblemForContest(solution.getPid());
    } else
    {
      problemModel = problemService.findProblem(solution.getPid());
    }
    
    setResult(ResultType.RUN, 0, 0);
    
    int timeLimit = problemModel.getTimeLimit();
    int memoryLimit = problemModel.getMemoryLimit();
    String cmd = buildCommand(timeLimit, memoryLimit);
    
    Process process = Runtime.getRuntime().exec(cmd);
    InputStream inputStream = process.getInputStream();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    StringBuilder stringBuilder = new StringBuilder();
    while ((line = bufferedReader.readLine()) != null)
    {
      stringBuilder.append(line);
    }
    bufferedReader.close();

    StringBuilder sb = new StringBuilder();
    InputStream errorStream = process.getErrorStream();
    while (errorStream.available() > 0)
    {
      Character c = new Character((char) errorStream.read());
      sb.append(c);
    }

    String[] resultStr = stringBuilder.toString().split(" ");
    int exitValue = process.waitFor();
    if (exitValue > 0)
    {
      updateSystemError(sb.toString());
      return false;
    }
    checkResult(resultStr, sb.toString());

    log.info(Printf.str("%d: Total run time: %d ms", solution.getSid(), solution.getTime()));
    synchronized (JudgeAdapter.class)
    {
      updateUser();
      if (!updateContest())
      {
        updateProblem();
      }
    }
    return true;
  }
  
  private String buildCommand(int timeLimit, int memoryLimit)
  {
    String workPath = judgeService.getWorkPath(solution);
    String dataPath = OjConfig.getString("dataPath");
    
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("/usr/local/bin/powerjudge");
    stringBuilder.append(" -s ");
    stringBuilder.append(solution.getSid());
    stringBuilder.append(" -p ");
    stringBuilder.append(solution.getPid());
    stringBuilder.append(" -l ");
    stringBuilder.append(solution.getLanguage());
    stringBuilder.append(" -t ");
    stringBuilder.append(timeLimit);
    stringBuilder.append(" -m ");
    stringBuilder.append(memoryLimit);
    stringBuilder.append(" -d ");
    stringBuilder.append(FileNameUtil.normalizeNoEndSeparator(workPath));
    stringBuilder.append(" -D ");
    stringBuilder.append(FileNameUtil.normalizeNoEndSeparator(dataPath));

    System.out.println(stringBuilder.toString());
    return stringBuilder.toString();
  }

  private void checkResult(String[] resultStr, String errorOut)
  {
    if (resultStr != null && resultStr.length >= 4)
    {
      try
      {
        int result = Integer.parseInt(resultStr[0]);
        int time = Integer.parseInt(resultStr[1]);
        int memory = Integer.parseInt(resultStr[2]);
        int test = Integer.parseInt(resultStr[3]);
        setResult(result, time, memory, test);
      } catch (NumberFormatException e)
      {
        updateSystemError(e.getLocalizedMessage());

        if (OjConfig.getDevMode())
          e.printStackTrace();
        log.error(e.getLocalizedMessage());
      }
    } else
    {
      updateSystemError(errorOut);
    }

    if (solution.getResult() == ResultType.CE)
    {
      updateCompileError(readError("stderr_compiler.txt"));
    } else if (solution.getResult() == ResultType.RE)
    {
      if (StringUtil.isBlank(errorOut))
      {
        errorOut = readError("stderr_executive.txt");
      }
      updateRuntimeError(errorOut);
    }
  }

  private String readError(String fileName)
  {
    String workPath = judgeService.getWorkPath(solution);
    StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try
    {
      br = new BufferedReader(new FileReader(judgeService.getWorkDirPath(solution) + fileName));
      String line;
      while ((line = br.readLine()) != null)
      {
        if (line.trim().startsWith(workPath))
        {
          line = line.substring(workPath.length());
        }
        sb.append(line).append('\n');
      }
    } catch (Exception e)
    {
      if (OjConfig.getDevMode())
        e.printStackTrace();
      log.error(e.getLocalizedMessage());
    } finally
    {
      if (br != null)
      {
        try
        {
          br.close();
        } catch (IOException ignored)
        {
        }
      }
    }

    return sb.length() > 0 ? sb.toString() : null;
  }

}
