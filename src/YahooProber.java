import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class QueryResult {
	int numHits;
	JSONArray topResults;
}

/**
 * Part of this code is from out first project
 */
public class YahooProber {

	// Yahoo API string
	private final String API_KEY = "BEWTNqTV34H1zojJNQ5MZB48A1vR2mJeNAhKRvk5.bLyZd6gYgQmsVVsqZ7vv32aW73O6VNyzTO";
	// Number of hierarchy levels
	private static int levels = 2;
	// Default url
	private String url = "hardwarecentral.com";
	// Default specificity
	private double SPECIFICITY = 0.6f;
	// Default coverage
	private int COVERAGE = 100;
	// Number of top results to use for content-summary
	private static final int topResults = 4;
	// Category tree
	private HashMap<String, String> catNodes = new HashMap<String, String>();
	// Cached results for queries
	private HashMap<String, QueryResult> cachedResults = new HashMap<String, QueryResult>();
	// Result for categories
	private HashMap<String, Double> catSpecificity = new HashMap<String, Double>();
	private HashMap<String, Integer> catCoverage = new HashMap<String, Integer>();
	private HashMap<String, Integer> overallCoverage = new HashMap<String, Integer>();

	/*
	 * Default constructor
	 */
	public YahooProber() {
		this.initalizeTree();
		this.classifyDB();
	}

	/*
	 * Constructor with arguments
	 */
	public YahooProber(String url, double specificity, int coverage) {
		this.url = url;
		this.SPECIFICITY = specificity;
		this.COVERAGE = coverage;
		this.initalizeTree();
		this.classifyDB();
	}

	public void initalizeTree() {
		this.catNodes.put("Sports", "Root");
		this.catNodes.put("Health", "Root");
		this.catNodes.put("Computers", "Root");
		this.catNodes.put("Basketball", "Sports");
		this.catNodes.put("Soccer", "Sports");
		this.catNodes.put("Fitness", "Health");
		this.catNodes.put("Diseases", "Health");
		this.catNodes.put("Hardware", "Computers");
		this.catNodes.put("Programming", "Computers");
	}

	public String getParent(String catNode) {
		return this.catNodes.get(catNode);
	}

	public int getOverallCoverage(String catNode) {

		return this.overallCoverage.get(catNode);
	}

	public int getCoverage(String catNode) {

		return this.catCoverage.get(catNode);
	}

	public LinkedList<String> classifyDB() {

		HashSet<String> pCategories = new HashSet<String>();
		HashSet<String> categories = new HashSet<String>();
		categories.add("queries/Root.txt");
		int level = 1;
		do {
			HashSet<String> tempCats = new HashSet<String>();
			for (String cat : categories) {
				String catName = cat.split("queries\\/|.txt")[1];
				System.err.println(cat);
				int coverage = 0;
				try {
					FileInputStream fstream = new FileInputStream(cat);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(in));
					String query;
					int cCoverage = 0;
					String pCat = null;
					while ((query = br.readLine()) != null) {
						String queryTerms[] = query.split(" ");
						if ((pCat != null) && (!queryTerms[0].equals(pCat))) {
							this.catCoverage.put(pCat, cCoverage);
							cCoverage = 0;
						}
						tempCats.add("queries/" + queryTerms[0] + ".txt");
						int numhits = this.poseQuery(queryTerms);
						//Try to avoid abusing the site
						Thread.sleep(5000);
						coverage += numhits;
						cCoverage += numhits;
						pCat = queryTerms[0];
					}
					this.catCoverage.put(pCat, cCoverage);
					in.close();
					this.overallCoverage.put(catName, coverage);
					if (this.getParent(catName) != null) {
						double specificity = this.getCoverage(catName)
								* 1.0f
								/ this.getOverallCoverage(this
										.getParent(catName));
						this.catSpecificity.put(catName, specificity);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			categories.clear();
			categories.addAll(tempCats);
			tempCats.clear();
			LinkedList<String> toRemove = new LinkedList<String>();
			for (String catName : categories) {
				String c = catName.split("queries\\/|.txt")[1];
				double specificity = this.getCoverage(c) * 1.0
						/ this.overallCoverage.get(this.getParent(c));
				if (this.getCoverage(c) < this.COVERAGE
						|| specificity < this.SPECIFICITY) {
					System.err.println("Mark to remove " + c + " "
							+ specificity + " " + this.getCoverage(c));
					toRemove.add(catName);
				}
			}
			for (String tr : toRemove) {
				String c = tr.split("queries\\/|.txt")[1];
				categories.remove(tr);
				this.catCoverage.remove(c);
			}
			level++;
		} while (level <= levels);

		for (String catName : categories) {
			String c = catName.split("queries\\/|.txt")[1];
			double specificity = this.getCoverage(c) * 1.0f
					/ this.getOverallCoverage(this.getParent(c));
			this.catSpecificity.put(c, specificity);
		}
		for (String fc : this.catSpecificity.keySet()) {
			System.err.println("sc " + fc + " " + this.catSpecificity.get(fc)
					+ " " + this.catCoverage.get(fc));
		}

		return null;
	}

	public int poseQuery(String queryTerms[]) { 

		String query = new String();
		JSONArray ja = new JSONArray();
		// Convert spaces to + to make a valid URL
		try {
			for (int i = 1; i < queryTerms.length; i++)
				query += " " + queryTerms[i];
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		JSONObject j = null;
		URL url;
		try {
			url = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + query
					+ "?appid=" + API_KEY + "&sites=" + this.url + "&start=0"
					+ "&count=" + this.topResults + "&format=json");
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
			String strHits = (String) (json.getJSONObject("ysearchresponse")
					.get("totalhits"));
			int numHits = Integer.parseInt(strHits);
			if (numHits == 0)
				return 0;
			ja = json.getJSONObject("ysearchresponse").getJSONArray(
					"resultset_web");

			// this.cachedResults.put(, value)
			/*
			 * System.out.println("\nQuerying for " + query); for (int i = 0; i
			 * < this.topResults; i++) { if (ja.length() == i) { break; }
			 * System.out.print((i + 1) + ". "); j = ja.getJSONObject(i);
			 * System.out.println(j.getString("title"));
			 * System.out.println(j.getString("url"));
			 * System.out.println(j.getString("abstract"));
			 * System.out.println("Yahoo URL = " + url.toString()); }
			 * 
			 * System.err.println("Number of matches for query " + query + " ="
			 * + numHits);
			 */

			return numHits;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void buildContentSummary() {

	}

	public static void main(String args[]) {
		if (args.length >= 3) {
			System.out.println("Using command line arguments...");
			float p = Float.parseFloat(args[1]);
			int c = Integer.parseInt(args[2]);
			new YahooProber(args[0], p, c);
		} else {
			System.out
					.println("No arguments provided..Using default database!");
			new YahooProber();
		}
	}
}