package com.example.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {


    ArrayList<String> titles;
    ArrayList<String> urlsArray;

    SQLiteDatabase articleDB;
    ArrayAdapter<String> arrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titles=new ArrayList<>();
        urlsArray=new ArrayList<>();

        articleDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR) ");
        DownloadTask downloadTask=new DownloadTask();
        try {
            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();

        } catch (Exception e) {
            e.printStackTrace();
        }

        ListView listView=findViewById(R.id.listView);



        arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (!DetectConnection.checkInternetConnection(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "An error occured while fetching data please check your internet connection!!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent=new Intent(getApplicationContext(),webcontent.class);
                intent.putExtra("url",urlsArray.get(position));
                startActivity(intent);}
            }
        });

        updateListView();

    }

    public void updateListView(){
        Cursor c=articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();

            do{
                titles.add(c.getString(titleIndex));
            }
            while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {

            URL url=null;
            HttpURLConnection connection=null;
            String result="";

            try {
                url=new URL(urls[0]);
                connection=(HttpURLConnection) url.openConnection();
                InputStream inputStream=connection.getInputStream();
                InputStreamReader reader=new InputStreamReader(inputStream);
                int data=reader.read();
                while (data!=-1){
                    result+=(char)data;
                    data=reader.read();
                }
                Log.i("news ids", result);

                int numberOfItems=20;

                JSONArray array = new JSONArray(result);

                if(array.length()<20){
                    numberOfItems=array.length();
                }
                //Log.i("array", array.toString());

                articleDB.execSQL("DELETE FROM articles");
                for (int i = 0; i < numberOfItems; i++) {
                    String articleId= array.getString(i);
                    String articleInfo="";
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleId+".json?print=pretty");
                    connection=(HttpURLConnection) url.openConnection();
                    inputStream=connection.getInputStream();
                    reader=new InputStreamReader(inputStream);
                    data=reader.read();
                    while (data!=-1){
                        articleInfo+=(char)data;
                        data=reader.read();
                    }

                    Log.i("article info",articleInfo);

                    JSONObject object=new JSONObject(articleInfo);

                    if(!object.isNull("title")&&!object.isNull("url")){
                        String articleTitle=object.getString("title");
                        String articleUrl=object.getString("url");

                        /*url=new URL(articleUrl);
                        connection=(HttpURLConnection) url.openConnection();
                        inputStream=connection.getInputStream();
                        reader=new InputStreamReader(inputStream);

                        String articleContentHtml="";
                        data=reader.read();
                        while (data!=-1){
                            articleContentHtml+=(char)data;
                            data=reader.read();
                        }*/

                        urlsArray.add(articleUrl);
                        String sql="INSERT INTO articles (articleId,title) VALUES(?,?)";
                        SQLiteStatement sqLiteStatement=articleDB.compileStatement(sql);
                        sqLiteStatement.bindString(1,articleId);
                        sqLiteStatement.bindString(2,articleTitle);
                        sqLiteStatement.execute();///wtf i missed it 2 hrs struggling through it

                    }
                }

                return  result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            System.out.println(titles);
            System.out.println(urlsArray);

            updateListView();
        }
    }


}
