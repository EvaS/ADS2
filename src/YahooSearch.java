import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Part of this code is from out first project
 */
public class YahooSearch {

	private final String API_KEY = "BEWTNqTV34H1zojJNQ5MZB48A1vR2mJeNAhKRvk5.bLyZd6gYgQmsVVsqZ7vv32aW73O6VNyzTO";
    private String url="diabetes.org";
    private float specificity=0.6f;
    private int coverage=100;
	
	public YahooSearch() {
	}
	
	public YahooSearch(String url,float specificity, int coverage) {
		this.url="http://"+url;
		this.specificity=specificity;
		this.coverage=coverage;
	}
	
	public void makeQuery(String query) {
		System.out.println("\nQuerying for " + query);
		JSONArray ja = new JSONArray();
		// Convert spaces to + to make a valid URL
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		JSONObject j = null;
		URL url;
		try {
			url = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + query
					+ "?appid=" + API_KEY + "&start=0"
					+ "&count=50&format=json");
			URLConnection connection = url.openConnection();
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			String response = builder.toString();
			JSONObject json = new JSONObject(response);
			ja = json.getJSONObject("ysearchresponse").getJSONArray(
					"resultset_web");
			for (int i = 0; i < 10; i++) {
				System.out.print((i + 1) + ". ");
				j = ja.getJSONObject(i);
				System.out.println(j.getString("title"));
				System.out.println(j.getString("url"));
				System.out.println(j.getString("abstract"));
				System.out.println("Yahoo URL = " + url.toString());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		if (args.length >= 3) {
			System.out.println("Using command line arguments...");
			float p = Float.parseFloat(args[1]);
			int c = Integer.parseInt(args[2]);
			new YahooSearch(args[0], p, c);
		} else {
			System.out.println("No arguments provided..Using default database!");
			new YahooSearch();
		}
	}
}
