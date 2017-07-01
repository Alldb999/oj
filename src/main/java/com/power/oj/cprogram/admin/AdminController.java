package com.power.oj.cprogram.admin;

import com.jfinal.aop.Before;
import com.jfinal.ext.interceptor.POST;
import com.jfinal.plugin.activerecord.Record;
import com.power.oj.contest.ContestService;
import com.power.oj.contest.model.ContestModel;
import com.power.oj.core.OjConfig;
import com.power.oj.core.OjController;
import com.power.oj.cprogram.CProgramConstants;
import com.power.oj.cprogram.CProgramService;
import com.power.oj.user.UserService;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by w7037 on 2017/6/14.
 */
@RequiresPermissions("teacher")
public class AdminController extends OjController{
    private Integer GetType() {
        Integer type = getParaToInt("type");
        if(type == null) {
            type = 0;
        }
        setAttr("type", type);
        return type;
    }
    private String buildQuery() {
        String query = new String();
        Integer cid = getParaToInt("cid");
        if(cid != null) {
            query += "&cid=" + cid;
            setAttr("cid", cid);
        }
        Integer week = getParaToInt("week");
        if(week != null) {
            query += "&week=" + week;
            setAttr("WEEK", week);
        }
        Integer lecture = getParaToInt("lecture");
        if(lecture != null) {
            query += "&lecture=" + lecture;
            setAttr("LECTURE", lecture);
        }
        return query;
    }
    private void main() {
        setAttrs(com.power.oj.admin.AdminService.me().getSystemInfo());
    }
    private void add(int type) {
        setAttr("teacherList", CProgramService.GetTeacherList());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long ctime = OjConfig.startInterceptorTime + 3600000;
        setAttr("startTime", sdf.format(new Date(ctime)));
        if(type == ContestModel.TYPE_WORK) {
            setAttr("endTime", sdf.format(new Date(ctime + 7 * 24 * 3600 * 1000)));
        }
        else {
            setAttr("endTime", sdf.format(new Date(ctime + 2 * 3600 * 1000)));
        }
    }
    private void edit() {
        setAttr("techerList", CProgramService.GetTeacherList());
    }
    private void manager() {
        Integer cid = getParaToInt("cid");
        List<Record> problems = ContestService.me().getContestProblems(cid, UserService.me().getCurrentUid());
        setAttr("problems", problems);
        setAttr("number", 50);
    }
    private void score() {
        Integer type = getParaToInt("type");
        Integer cid = getParaToInt("cid");
        Integer week = getParaToInt("week");
        Integer lecture = getParaToInt("lecture");
        Integer Rate = getParaToInt("rate", 30);
        if(cid == -1) {
            List<Record> contest = AdminService.GetAllWorkContest(type, week, lecture);
            List<Integer> contestList = new ArrayList<>();
            for(Record c : contest) {
                contestList.add(c.get("cid"));
            }
            List<Record> user = AdminService.GetAllScoreList(type, week, lecture, Rate, contest.size());
            setAttr("workList", contestList);
            setAttr("allList", user);
            return;
        }
        setAttr("scoreList", AdminService.GetScoreList(type, cid, week, lecture));
    }
    public void index() {
        Integer type = GetType();
        String action = getPara("action","");
        if(type == 0) {
            main();
            return;
        }
        List<Record> contestList = AdminService.GetContestListForSelect(type);
        setAttr("contestList", contestList);
        setAttr("weeks", CProgramConstants.weeks);
        setAttr("lectures", CProgramConstants.lecture);
        setAttr("action", action);
        String query = buildQuery();
        setAttr("query", query);
        if(action.equals("add")) {
            add(type);
        }
        else {
            Integer cid = getParaToInt("cid");
            if(cid == null) return;
            if(cid != -1) {
                ContestModel contest = ContestService.me().getContest(cid);
                setAttr("contest", contest);
            }
            if(action.equals("edit")) {
                edit();
            }
            if(action.equals("manager")) {
                manager();
            }
            if(action.equals("score")) {
                score();
            }
        }
    }

    public void getxls() {
        Integer type = getParaToInt("type");
        Integer cid = getParaToInt("cid");
        Integer week = getParaToInt("week");
        Integer lecture = getParaToInt("lecture");
        Integer Rate = getParaToInt("rate", 30);
        if(cid == -1) {
            List<Record> contest = AdminService.GetAllWorkContest(type, week, lecture);
            List<Integer> contestList = new ArrayList<>();
            for(Record c : contest) {
                contestList.add(c.get("cid"));
            }
            List<Record> user = AdminService.GetAllScoreList(type, week, lecture, Rate, contest.size());
            try {
                DateFormat fmtDateTime = new SimpleDateFormat("yyyyMMdd");
                String fileName = UserService.me().getCurrentUser().getRealName() +
                        CProgramConstants.weeks.get(week) +
                        CProgramConstants.lecture.get(lecture);
                if(type == ContestModel.TYPE_EXPERIMENT_EXAM) {
                    fileName += "实验";
                }
                else {
                    fileName += "课程";
                }
                fileName += "成绩表" + fmtDateTime.format(new Date());
                File file = new File( fileName + ".xls");
                WritableWorkbook book =
                        Workbook.createWorkbook(file);
                WritableSheet sheet = book.createSheet("sheet1",0);
                sheet.addCell(new Label(0,0, fileName));
                sheet.mergeCells(0, 0, 5 + contestList.size(), 0);
                sheet.addCell(new Label(0, 1, "用户名"));
                sheet.addCell(new Label(1, 1, "姓名"));
                sheet.addCell(new Label(2, 1, "学号"));
                sheet.addCell(new Label(3, 1, "班级"));
                for(int i = 1; i <= contestList.size(); i++) {
                    if(type == ContestModel.TYPE_EXPERIMENT_EXAM) {
                        sheet.addCell(new Label(i + 3, 1, "实验" + i));
                    }
                    else {
                        sheet.addCell(new Label(i + 3, 1, "作业" + i));
                    }
                }
                if(type == ContestModel.TYPE_EXPERIMENT_EXAM) {
                    sheet.addCell(new Label(contestList.size() + 4, 1, "实验考试成绩"));
                }
                else {
                    sheet.addCell(new Label(contestList.size() + 4, 1, "课程考试成绩"));
                }
                sheet.addCell(new Label(contestList.size() + 5, 1, "总成绩"));

                Integer idx = 2;
                for(Record u : user) {
                    sheet.addCell(new Label(0, idx, u.getStr("name")));
                    sheet.addCell(new Label(1, idx, u.getStr("realName")));
                    sheet.addCell(new Label(2, idx, u.getStr("stuid")));
                    sheet.addCell(new Label(3, idx, u.getStr("Class")));
                    Integer row = 4;
                    for(Integer c : contestList) {
                        Map<Integer, Integer> scoreMap = u.get("scoreMap");
                        Integer sc = scoreMap.get(c);
                        if(sc == null) {
                            sc = 0;
                        }
                        sheet.addCell(new Label(row++, idx,  sc.toString()));
                    }
                    sheet.addCell(new Label(row++, idx,  u.getInt("examScore").toString()));
                    sheet.addCell(new Label(row++, idx, u.getInt("finalScore").toString()));
                    idx++;
                }
                book.write();
                book.close();
                renderFile(file);
            }
            catch (Exception e) {
                renderNull();
            }
        }
        else {
            renderNull();
        }
    }

    @Before(POST.class)
    public void save() {
        Integer type = getParaToInt("type", ContestModel.TYPE_WORK);
        String startTime = getPara("startTime");
        String endTime = getPara("endTime");
        ContestModel contestModel = getModel(ContestModel.class, "contest");
        contestModel.put("type", type);
        contestService.addContest(contestModel, startTime, endTime);
        Integer cid = contestModel.getCid();
        String query = "&cid=" + cid;
        if(type != ContestModel.TYPE_EXPERIMENT) {
            query += "&week=" + contestModel.getLockBoardTime();
            query += "&lecture=" + contestModel.getUnlockBoardTime();
        }
        String action = "&action=manager";
        redirect("/cprogram/admin/?type=" + type + query + action);
    }
    @Before(POST.class)
    public void update() {
        Integer cid = getParaToInt("cid");
        String startTime = getPara("startTime");
        String endTime = getPara("endTime");
        ContestModel contestModel = contestService.getContest(cid);
        ContestModel newContestModel = getModel(ContestModel.class, "contest");
        contestModel.put(newContestModel);
        contestService.updateContest(contestModel, startTime, endTime);
        String query = buildQuery();
        Integer type = GetType();
        String action = "&action=manager";
        redirect("/cprogram/admin/?type=" + type + query + action);
    }
    @Before(POST.class)
    public void search() {
        String query = buildQuery();
        Integer type = GetType();
        String action = "&action=score";
        redirect("/cprogram/admin/?type=" + type + query + action);
    }
}
