package com.example.android.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();

    SQLiteDatabase articlesDB;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    ArrayList<String> urls = new ArrayList<String>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleUrl", urls.get(position));
                startActivity(i);

                // Log.i("articleURL", urls.get(position));
            }
        });


        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();



        try {
            String result = task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            //above line is moved up to sync the task
            JSONArray jsonArray = new JSONArray(result);

            articlesDB.execSQL("DELETE FROM articles");
            //replace 20 with jsonArray.length()
            for( int i = 0; i < 20; i++){


                String articleId = jsonArray.getString(i);

                DownloadTask getArticle = new DownloadTask();
                String articleInfo = getArticle.execute("https://hacker-news.firebaseio.com/v0/item/"+ articleId +".json?print=pretty").get();


                JSONObject jsonObject = new JSONObject(articleInfo);
                Log.i("ArticleID", jsonObject.toString());

                String articleTitle = jsonObject.getString("title");
                String articleURL = jsonObject.getString("url");

                articleIds.add(Integer.valueOf(articleId));
                articleTitles.put(Integer.valueOf(articleId), articleTitle);
                articleURLs.put(Integer.valueOf(articleId), articleURL);
                //use of Prepared Statement to avoid issues while inserting data into SQL such as (')quotes coming in titles. SQL treats it as end of string, while it is not.

                //String sql = "INSERT INTO articles (articleId, url, title) VALUES (" +articleId+", '"+articleURL+"', '"+articleTitle+"')";
                String sql = "INSERT INTO articles (articleId, url, title) VALUES (?, ?, ?)";
                //fix for the above comment
                SQLiteStatement statement = articlesDB.compileStatement(sql);

                statement.bindString(1, articleId);
                statement.bindString(2, articleURL);
                statement.bindString(3, articleTitle);
                // this needs to be replace.  articlesDB.execSQL();
                statement.execute();

                //Log.i("articleTitle", articleTitle);
                //Log.i("articleURL", articleURL);


            }
            //Log.i("ArticleIds", articleIds.toString());
            //Log.i("ArticleTitles", articleTitles.toString());
            //Log.i("ArticleURLs", articleURLs.toString());
            updateListView();
           // Log.i("Result", result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateListView(){

        try {


            Cursor c = articlesDB.rawQuery("SELECT * FROM ARTICLES ORDER BY articleId DESC", null);

            int articleIdIndex = c.getColumnIndex("articleId");
            int titleIndex = c.getColumnIndex("title");
            int urlIndex = c.getColumnIndex("url");

            c.moveToFirst();
            titles.clear(); // make sure its empty
            urls.clear();

            while (c != null) {

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));

                //Log.i("articleID", Integer.toString(c.getInt(articleIdIndex)));
                //Log.i("articleTitle", c.getString(titleIndex));
                //Log.i("articleURL", c.getString(urlIndex));

                c.moveToNext();

            }
            arrayAdapter.notifyDataSetChanged();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try{

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while(data != -1){
                    char current = (char) data;

                    result = result + current;

                    data = reader.read();
                }


            }
            catch (Exception e){

                e.printStackTrace();
            }

            return result;

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
