/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renamer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Cwlin
 */
public class TestParseMovieName {

    public static void main(String args[]) {
        //String result = "我的火星小孩   The Martian Child";
        //String result = "間諜橋 Bridge of Spies	";
        //String result = "猩球崛起：黎明的進擊 Dawn of the Planet of the Apes ";
        //String result = "腦筋急轉彎 Inside Out	";
        String result = "007：惡魔四伏 Spectre";
        //String result = "美國Ｘ檔案 American History X";
        result = result.trim();
       // "[^\\x00-\\x40\\x5B-\\x60\\x7B-\\x7F]"
        Pattern p = Pattern.compile("(([0-9A-Za-z]|[^\\x00-\\x40\\x5B-\\x60\\x7B-\\x7F])+) ([A-Za-z0-9:.,&\\- '\\[\\]]+)");
        Matcher m = p.matcher(result);
        if (m.matches()) {
            System.out.println(m.group(1));
            System.out.println(m.group(3));
        }
    }
}
