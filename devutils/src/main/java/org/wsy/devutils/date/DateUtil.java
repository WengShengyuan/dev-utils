package org.wsy.devutils.date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 快速获取日期工具
 *
 * @author wengshengyuan
 * @version 1.0.0
 * @ClassName DateUtil
 * @Date 2017年9月20日 上午8:33:55
 */
public class DateUtil {

    /**
     * 当前日期    减去  以前日期  = 过去几天了
     *
     * @param fromDate 以前日期
     * @param toDate   当前日期
     * @return
     */
    public static int dateDiff(Date fromDate, Date toDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(fromDate);
        int day1 = c.get(Calendar.DAY_OF_YEAR);
        c.setTime(toDate);
        int day2 = c.get(Calendar.DAY_OF_YEAR);
        return day2 - day1;
    }

    /**
     * 转为String
     *
     * @param date
     * @param format
     * @return
     */
    public static String format(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String string = sdf.format(date);
        return string;
    }

    /**
     * 获取当前日期(0点0分0秒)
     *
     * @return
     */
    public static Date today() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date zero = calendar.getTime();
        return zero;
    }

    /**
     * 推算日期
     *
     * @param date
     * @param days
     * @return
     */
    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    /**
     * 获取时间戳
     * 输出结果:1438692801766
     */
    public long getTimeStamp() {

        return System.currentTimeMillis();

    }

    /**
     * 将时间戳转化为标准时间
     * 输出：Tue Oct 07 12:04:36 CST 2014
     */
    public Date timestampToDate(long times){

        return  new Date(times);

    }
    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyy-MM-dd HH:mm:ss
     */
    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 获取现在时间
     *
     * @return 返回短时间字符串格式yyyy-MM-dd
     */
    public static String getStringDateShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
     *
     * @param dateDate
     * @return
     */
    public static String dateToStrLong(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(dateDate);
        return dateString;
    }

    /**
     * 将短时间格式时间转换为字符串 yyyy-MM-dd
     *
     * @param dateDate
     * @return
     */
    public static String dateToStr(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(dateDate);
        return dateString;
    }


    /**
     * 得到现在时间
     *
     * @return
     */
    public static Date getNow() {
        Date currentTime = new Date();
        return currentTime;
    }

    /**
     * 得到二个日期间的间隔天数
     */
    public static String getTwoDay(String sj1, String sj2) {
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
        long day = 0;
        try {
            Date date = myFormatter.parse(sj1);
            Date mydate = myFormatter.parse(sj2);
            day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return "";
        }
        return day + "";
    }

    /**
     * 得到一个时间延后或前移几天的时间,nowdate为时间,delay为前移或后延的天数
     */
    public static String getNextDay(Date nowdate, String delay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(nowdate);
        cal.add(Calendar.DATE, Integer.valueOf(delay));
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        String preDate = dft.format(cal.getTime());
        return preDate;


    }

    /**
     * 获取下一月yyyy-MM-dd
     */
    public static String getPreMonth(Date srcdate,int m) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(srcdate);
        cal.add(Calendar.MONTH, m);
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        String preMonth = dft.format(cal.getTime());
        return preMonth;
    }
    /**
     * 获取下一年yyyy-MM-dd
     */
    public static String getPreYear(Date srcdate,int y) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(srcdate);
        cal.add(Calendar.YEAR, y);
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        String preMonth = dft.format(cal.getTime());
        return preMonth;
    }

    public static void main(String[] args) {
        System.out.println(getNextDay(new Date(),"2"));
    }
}
