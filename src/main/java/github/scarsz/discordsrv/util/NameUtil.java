package github.scarsz.discordsrv.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class NameUtil
{
    public static void init()
    {
        try (Connection con =  DiscordSRV.getSql().getConnection();
            Statement stmt = con.createStatement();)
        {
            stmt.execute("CREATE TABLE IF NOT EXISTS PLAYER(" +
                    "UUID text," +
                    "NAME text," +
                    "UPDATE integer" +
                    ")");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void insert(UUID id, String name)
    {
        try (Connection con = DiscordSRV.getSql().getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO PLAYER VALUES(?, ?, ? )" +
                     " ON CONFLICT DO " +
                     "UPDATE SET NAME=?, UPDATE=?"))
        {
            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            stmt.setLong(3, new Date().getTime());
            stmt.setString(4, name);
            stmt.setLong(5, new Date().getTime());
            stmt.execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public static String fetch(UUID uuid, String name)
    {
        insert(uuid, name);
        return name;
    }

    public static String fetch(UUID id)
    {
        String name;

        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" +
                    id.toString().replace("-", "")).openConnection();

            connection.setRequestMethod("GET");

            connection.connect();

            if (connection.getResponseCode() != 200)
                return null;

            try(InputStream stream = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(stream))
            {
                JsonObject object = new Gson().fromJson(reader, JsonObject.class);
                name = object.get("name").getAsString();
            }

            insert(id, name);
            return name;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String getName(UUID id)
    {
        String name = null;
        int updated;

        if (id == null)
            return "";

        try(Connection con = DiscordSRV.getSql().getConnection();
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM PLAYER WHERE UUID=?"))
        {
            stmt.setString(1, id.toString());
            ResultSet set = stmt.executeQuery();

            if (set.next())
            {
                name = set.getString("NAME");
                updated = set.getInt("UPDATE");

            }
            else
                updated = 0;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }

        if (name == null)
            return fetch(id);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -25);

        if (updated < calendar.getTime().getTime())
            return fetch(id);

        return name;
    }
}
