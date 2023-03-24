package com.example;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.*;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static java.sql.JDBCType.NULL;

@Controller
public class Mapping {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<Map<String, Object>>  selectAllSongDB() {

        String sql = "SELECT * FROM thorsten_music.song;";
        List<Map<String, Object>> list =  jdbcTemplate.queryForList(sql);

        return list;
    }
    private List<Map<String, Object>>  selectSongFromCategory(String category) {

        java.lang.Object[] args = {category};

        String sql = "SELECT * FROM thorsten_music.song WHERE category = ?;";


        List<Map<String, Object>> list =  jdbcTemplate.queryForList(sql, args);

        return list;
    }


    private void  removeSongDB(String ID) {


        String sql = "DELETE FROM thorsten_music.song WHERE songID = ?;";
                jdbcTemplate.update(sql, Integer.parseInt(ID));

    }
    private void InsertSongDB(String name, String artist, String category, String year) {
        java.lang.Object[] args;
        if(!year.isEmpty()) {
            int intYear = Integer.parseInt(year);
            args = new java.lang.Object[]{name, artist, category, intYear};
        }else {
            args = new java.lang.Object[]{name, artist, category, 0};
        }


        String sql = "INSERT INTO thorsten_music.song (`name`, `artist`, `category`, `year`) VALUES (?, ?, ?, ?);";
        jdbcTemplate.update(sql, args);
    }
    private void UpdateSongDB(String name, String artist, String category, String year, int ID) {

        java.lang.Object[] args;
        if(!year.isEmpty()) {
            int intYear = Integer.parseInt(year);
            args = new java.lang.Object[]{name, artist, category, intYear, ID};
        }else {
            args = new java.lang.Object[]{name, artist, category, 0, ID};
        }
        String sql = "UPDATE thorsten_music.song\n" +
                "SET name = ?, artist = ?, category = ?, year = ?\n" +
                "WHERE songID = ?;";
        jdbcTemplate.update(sql, args);
    }
    private List<Map<String, Object>>  selectAllUsersDB() {

        String sql = "SELECT * FROM thorsten_music.user;";
        List<Map<String, Object>> list =  jdbcTemplate.queryForList(sql);

        return list;
    }


    @GetMapping("/admin")
    public String loginaHandler(Model model) {
        //System.out.println(getDBTest());
        model.addAttribute("DBresponse", selectAllSongDB());

        return "admin";
    }
    @GetMapping("/user")
    public String userHandler(Model model) {
        model.addAttribute("DBresponse", selectAllSongDB());

        return "user";
    }
    @GetMapping("/login")
    public String loginHandler(Model model) {

        return "login";
    }


    @PostMapping("/login")
    public @ResponseBody Boolean attemptLogin(@RequestBody String userInfo) {
        String username = userInfo.substring(0, userInfo.indexOf('§'));
        String password = userInfo.substring(userInfo.indexOf('§')+1, userInfo.length());


        return authLogin(username, password);
    }

    @PostMapping("/updateDB")
    public @ResponseBody List<Map<String, Object>> updateDB(@RequestBody String stringTable) {


        List<Map<String, Object>> HTTPlist = stringToMapList(stringTable);
        List<Map<String, Object>> DBlist = selectAllSongDB();

        compateTables(HTTPlist, DBlist);

        return selectAllSongDB();
    }
    @PostMapping("/selectFromCategory")
    public @ResponseBody List<Map<String, Object>> selectFromCategory(@RequestBody String value) {

        if(value.equals("alla")) {
            return selectAllSongDB();
        }

        return selectSongFromCategory(value);
    }

    private boolean authLogin(String username, String password) {
        List<Map<String, Object>> userList = selectAllUsersDB();

        for(int i = 0; i < userList.size(); i++) {
            Map<String, Object> row = userList.get(i);
            if(username.equals(row.get("username"))) {
                if(password.equals(row.get("password"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void compateTables(List<Map<String, Object>> HTTPlist, List<Map<String, Object>> DBlist) {

    for(int i = 0; i < HTTPlist.size(); i++) {

        Map<String, Object> rowMap = HTTPlist.get(i);
        String ID = rowMap.get("songID").toString();
        //this loop just gets me the index of the map with maching ID

        String name = rowMap.get("name").toString();
        String artist = rowMap.get("artist").toString();
        String category = rowMap.get("category").toString();
        String year = rowMap.get("year").toString();

        System.out.println(rowMap);
        System.out.println("" + rowMap.get("name").toString().isEmpty());
        System.out.println("" + rowMap.get("artist").toString().isEmpty());
        System.out.println("" + rowMap.get("category").toString().isEmpty());

        int index = getIndex(ID, DBlist);
        if(index == 400) {
            System.out.println("insert row in db");

            InsertSongDB(name, artist, category, year);
        } else {

        Map<String, Object> DBrowMap = DBlist.get(index);
        if(
                        DBrowMap.get("name").equals(rowMap.get("name")) &&
                        DBrowMap.get("artist").equals(rowMap.get("artist")) &&
                        DBrowMap.get("category").equals(rowMap.get("category")) &&
                        DBrowMap.get("year").toString().equals(rowMap.get("year"))
        ) {
            System.out.println("No changes made");

        }else if(rowMap.get("name").toString().isEmpty() &&
                rowMap.get("artist").toString().isEmpty() &&
                rowMap.get("category").toString().isEmpty()) {
            removeSongDB(ID);

        }


        else {
            System.out.println("update db where id = " + ID);
            UpdateSongDB(name, artist, category, year, Integer.parseInt(ID));

        }
        }
    }
    System.out.println("NEW ROW");
}
    private int getIndex(String ID, List<Map<String, Object>> DBlist) {
    for(int j = 0; j < DBlist.size(); j++) {
        if(DBlist.get(j).get("songID").toString().equals(ID)) {
            return j;

        }
    }
    return 400;
}
    private void printList(List<Map<String, Object>> list){
    for (int i = 0; i < list.size(); i++) {
        for (Map.Entry<String, Object> entry : list.get(i).entrySet()) {
            System.out.print(entry.getKey() + ": " + entry.getValue().toString() + " ");
        }
        System.out.println(" ");
        
    }

}

    private List<Map<String, Object>> stringToMapList(String HTTPstring) {

        List<Map<String, Object>> list = new ArrayList<>();

    //will break if user inputs {
    int amountOfRows = HTTPstring.length() - HTTPstring.replace("{", "").length();

    String stringRemaining = HTTPstring;

    for(int i = 0; i < amountOfRows; i++ ) {

        String rowString = stringRemaining.substring(stringRemaining.indexOf('{'), stringRemaining.indexOf('}')+1);
        stringRemaining = stringRemaining.replace(rowString, "");



        try {
            Map<String, Object> mapping = new ObjectMapper().readValue(rowString, HashMap.class);
            list.add(mapping);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    return list;
}


   /* private JSONObject readJsonFile() throws Exception {
    File file = new File("src/main/resources/userFiles/users.json");
    String content = FileUtils.readFileToString(file, "utf-8");

    // Convert JSON string to JSONObject
    JSONObject tomJsonObject = new JSONObject(content);

    return tomJsonObject;


}
    private void jsonToFile() {
    String path = "src/main/resources/userFiles/users.json";

    String fileNamePath = "src/main/resources/";
    Path pathToFile = Paths.get(fileNamePath);

    JSONObject json = new JSONObject();
    try {
        json.put("joipoi", "musse2010");
        json.put("straight_spoon", "karthop9");

    } catch (JSONException e) {
        e.printStackTrace();
    }

    try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
        out.write(json.toString());
    } catch (Exception e) {
        e.printStackTrace();
    }

} */




}