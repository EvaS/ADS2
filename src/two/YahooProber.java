package two;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DocumentSample {
	String category;
	Set<String> filteredURLs = new HashSet<String>();
	// Stores the global word set of all the included documents
	Set<String> wordSet = new TreeSet<String>();
	Map<String, Set<String>> urlMap = new HashMap<String, Set<String>>();
}

/**
 * Part of this code is from out first project
 */
public class YahooProber {

	// Yahoo API string
	private String API_KEY = "BEWTNqTV34H1zojJNQ5MZB48A1vR2mJeNAhKRvk5.bLyZd6gYgQmsVVsqZ7vv32aW73O6VNyzTO";
	// Number of hierarchy levels
	private static int levels = 2;
	// Default url
	private String databaseURL = "java.sun.com";
	// Default specificity
	private double SPECIFICITY = 0.6;
	// Default coverage
	private long COVERAGE = 100;
	// Number of top results to use for content-summary
	private static final int topResults = 4;
	// Category tree
	private HashMap<String, String> catNodes = new HashMap<String, String>();
	// Cached results for queries
	private HashMap<String, Set<String>> cachedResults = new HashMap<String, Set<String>>();
	// Results for categories
	private HashMap<String, Double> catSpecificity = new HashMap<String, Double>();
	private HashMap<String, Long> catCoverage = new HashMap<String, Long>();
	private HashMap<String, Long> overallCoverage = new HashMap<String, Long>();

	/**
	 * Default constructor
	 */
	public YahooProber() {
		this.initalizeTree();
		this.qProbe();
	}

	/**
	 * Constructor with arguments
	 */
	public YahooProber(String url, double specificity, long coverage, String appId) {
		this.databaseURL = url;
		this.SPECIFICITY = specificity;
		this.COVERAGE = coverage;
		this.API_KEY=appId;
		this.initalizeTree();
		this.qProbe();
	}

	/**
	 * Initialize category tree
	 */
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

	/*
	 * Getters for class members
	 */
	public String getParent(String catNode) {
		return this.catNodes.get(catNode);
	}

	public long getOverallCoverage(String catNode) {
		return this.overallCoverage.get(catNode);
	}

	public long getCoverage(String catNode) {
		return this.catCoverage.get(catNode);
	}

	public double getSpecificity(String catNode) {
		return this.catSpecificity.get(catNode);
	}

	/**
	 * Checks whether a category was further pushed down
	 * during classification
	 */
	public boolean hasChild(String cat) {

		for (String c : this.catNodes.keySet()) {
			if (this.getParent(c).equals(cat)
					&& this.catSpecificity.containsKey(c))
				return true;
		}
		return false;
	}
	
	/**
	 * Implements QProber basic steps
	 */
	public void qProbe() {
		this.classifyDB();
		this.buildContentSummary();
	}

	/**
	 * Categorizes the database
	 */
	public LinkedList<String> classifyDB() {

		HashSet<String> categories = new HashSet<String>();
		categories.add("queries/Root.txt");
		int level = 1;
		System.out.println("Classifying ...");
		this.catSpecificity.put("Root", 1.0);
		// For all levels of the category-hierarchy
		do {
			// for storing child categories while iterating
			HashSet<String> tempCats = new HashSet<String>();
			Set<String> docs = null;
			for (String cat : categories) {
				String catName = cat.split("queries\\/|.txt")[1];
				if (!cachedResults.containsKey(catName)) {
					docs = new HashSet<String>();
					cachedResults.put(catName, docs);
				}
				long coverage = 0;
				try {
					FileInputStream fstream = new FileInputStream(cat);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(in));
					String query;
					long cCoverage = 0;
					String previousCategory = null;
					while ((query = br.readLine()) != null) {
						String[] queryTerms = query.split(" ");
						if ((previousCategory != null)
								&& (!queryTerms[0].equals(previousCategory))) {
							this.catCoverage.put(previousCategory, cCoverage);
							cCoverage = 0;
						}
						tempCats.add("queries/" + queryTerms[0] + ".txt");
						int numhits, tries = 0;
						// Try to fetch results until no exception occurs
						while (((numhits = this.poseQuery(queryTerms, docs)) == -1)
								&& (tries < 100))
							tries++;
						numhits = this.poseQuery(queryTerms, docs);
						coverage += numhits;
						cCoverage += numhits;
						previousCategory = queryTerms[0];
					}
					// Add the coverage of the last sub-category
					this.catCoverage.put(previousCategory, cCoverage);
					in.close();
					// Compute the overall coverage of this category
					this.overallCoverage.put(catName, coverage);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			categories.clear();
			categories.addAll(tempCats);
			tempCats.clear();
			LinkedList<String> toRemove = new LinkedList<String>();
			// Check the specificity and coverage of each category
			for (String catName : categories) {
				String c = catName.split("queries\\/|.txt")[1];
				double specificity = (this.getCoverage(c) * this
						.getSpecificity(this.getParent(c)))
						/ this.overallCoverage.get(this.getParent(c));
				this.catSpecificity.put(c, specificity);
				if (this.getCoverage(c) < this.COVERAGE
						|| specificity < this.SPECIFICITY) {
					toRemove.add(catName);
				}
				System.out.println("Coverage for category:" + c + " is "
						+ this.getCoverage(c));
				System.out.println("Specificity for category:" + c + " is "
						+ specificity);

			}
			// Remove all categories which are < Ts or Tc
			for (String tr : toRemove) {
				String c = tr.split("queries\\/|.txt")[1];
				categories.remove(tr);
				this.catCoverage.remove(c);
				this.catSpecificity.remove(c);
			}
			level++;
		} while (level <= levels);

		System.out.println("\n");

		// Classified categories
		System.out.println("Classification:");
		for (String fc : this.catSpecificity.keySet()) {
			if (this.hasChild(fc))
				continue;
			String fullCat = new String(fc);
			String p = this.getParent(fc);
			// Print full path for classified categories
			while (p != null) {
				fullCat = p + "/" + fullCat;
				p = this.getParent(p);
			}
			System.out.println(fullCat);
		}
		System.out.println("\n");
		return null;
	}

	/**
	 * Build content summaries
	 */
	public void buildContentSummary() {

		/* document sampling after category selection */
		Map<String, DocumentSample> docSample = new HashMap<String, DocumentSample>();
		DocumentSample dc;
		List<String> childList = new ArrayList<String>();
		// For all classified categories
		for (String fc : this.catSpecificity.keySet()) {
			dc = new DocumentSample();
			// category for this document sample
			dc.category = fc;
			// all the documents related to this category
			dc.filteredURLs = cachedResults.get(fc);
			docSample.put(fc, dc);
			String tempCat = fc;
			String parent = null;
			while ((parent = this.getParent(tempCat)) != null) {
				childList.add(tempCat);
				// fix for multiple children: check whether this parent has been
				// visited previously
				if (docSample.containsKey(parent)) {
					Set<String> urls = docSample.get(tempCat).filteredURLs;
					Set<String> parentUrls = docSample.get(parent).filteredURLs;
					if (parentUrls != null && urls != null)
						parentUrls.addAll(urls);
				} else {
					dc = new DocumentSample();
					dc.category = parent;
					dc.filteredURLs = cachedResults.get(parent);
					// add all the child documents
					for (String c : childList) {
						// Leaf categories do not have samples
						if (cachedResults.get(c) == null)
							continue;
						dc.filteredURLs.addAll(docSample.get(c).filteredURLs);
					}
					// add to the document class list
					docSample.put(parent, dc);
				}
				tempCat = parent;
			}
			// prepare for the next sample
			childList.clear();
		}

		// fetch all the webpages for each doc sample
		for (DocumentSample ds : docSample.values()) {
			int docId = 0;
			// Skip leaf categories
			if (ds.filteredURLs == null)
				continue;
			System.out.println("Building document sample for " + ds.category);
			for (String url : ds.filteredURLs) {
				System.out.printf("[%d] Getting page: %s \n", (docId + 1), url);
				Set<String> words = URLProcessor.runLynx(url);
				ds.urlMap.put(url, words);
				ds.wordSet.addAll(words);
				docId++;
				//Small delay to avoid abusing the site
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			String fileName = 
				//"sample-" + 
			ds.category + "-" + this.databaseURL
					+ ".txt";
			// display result to content-summary file
			this.display(ds, fileName);
		}
		System.out.println("\n\nFinished generating document samples..");
	}

	/**
	 * Display the word-document matrix
	 */
	public void display(DocumentSample ds, String fileName) {
		FileWriter fstream;
		BufferedWriter out;
		//In case a url contains / character
		fileName=fileName.replaceAll("/", "-");
		try {
			fstream = new FileWriter(fileName);
			out = new BufferedWriter(fstream);
		//	out.write("Document Sample for " + ds.category + "\n");
			int docCount = 0;
			for (String word : ds.wordSet) {
				out.write(word + "#");
				for (String url : ds.filteredURLs) {
					Set<String> words = ds.urlMap.get(url);
					if (words.contains(word))
						docCount++;
				}
				out.write(docCount + "\n");
				docCount = 0;
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Poses query using Yahoo BOSS
	 * 
	 * @param queryTerms
	 * @param categoryName
	 * @return number of matches for the query
	 */
	public int poseQuery(String[] queryTerms, Set<String> docs) {

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
					+ "?appid=" + API_KEY + "&sites=" + this.databaseURL
					+ "&start=0" + "&count=" + YahooProber.topResults
					+ "&format=json");
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(2000);
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			String response = builder.toString();
			JSONObject json = new JSONObject(response);
			// Get number of matches
			String strHits = (String) (json.getJSONObject("ysearchresponse")
					.get("totalhits"));
			int numHits = Integer.parseInt(strHits);
			if (numHits == 0)
				return 0;

			ja = json.getJSONObject("ysearchresponse").getJSONArray(
					"resultset_web");

			for (int i = 0; i < YahooProber.topResults; i++) {
				if (ja.length() == i) {
					break;
				}
				j = ja.getJSONObject(i);
				if (docs != null)
					docs.add(j.getString("url"));
				else
					System.err
							.println("poseQuery : document object passed is null");
			}
			return numHits;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			e.printStackTrace();
			return -1;
		}
		// Handles HTTP response error code: 503
		catch (IOException e) {
			e.printStackTrace();
			return -1;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	/**
	 * Main function initiating qprobing
	 */
	public static void main(String args[]) {
		if (args.length ==4 ) {
			System.out.println("Using command line arguments...");
			double p = Double.parseDouble(args[1]);
			long c = Long.parseLong(args[2]);
			new YahooProber(args[0], p, c, args[3]);
		} else {
			System.out
					.println("No arguments provided..Using default database!");
			new YahooProber();
		}
	}
}