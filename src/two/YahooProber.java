package two;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DocumentSample {
	String category;
	Set<String> filteredURLs = new HashSet<String>();
	Map<Integer, String> urlIdMap = new HashMap<Integer, String>();
	Map<Integer, String> wordIdMap = new HashMap<Integer, String>();
	// Stores the global word set of all the included documents
	Set<String> wordSet = new TreeSet<String>();
	Map<String, Document> urlMap = new HashMap<String, Document>();
}

/**
 * maintained for each URL / document
 * 
 * @author aman
 * 
 */
class Document {
	String url;
	Set<String> words = new HashSet<String>();

	public Document(String url) {
		this.url = url;
	}
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
	private String databaseURL = "hardwarecentral.com";
	// Default specificity
	private double SPECIFICITY = 0.6f;
	// Default coverage
	private int COVERAGE = 100;
	// Number of top results to use for content-summary
	private static final int topResults = 4;
	// Category tree
	private HashMap<String, String> catNodes = new HashMap<String, String>();
	// Cached results for queries
	private HashMap<String, Set<String>> cachedResults = new HashMap<String, Set<String>>();

	// Result for categories
	private HashMap<String, Double> catSpecificity = new HashMap<String, Double>();
	private HashMap<String, Integer> catCoverage = new HashMap<String, Integer>();
	private HashMap<String, Integer> overallCoverage = new HashMap<String, Integer>();

	// List of stopwords:
	// http://www.textfixer.com/resources/common-english-words.txt
	private static final String STOP_WORDS = "a aa aaa about above across after again against all almost alone along "
			+ "already also although always among an and another any anybody anyone "
			+ "anything anywhere are area areas as ask asked asking asks at "
			+ "away b back backed backing backs be became because become becomes "
			+ "been before began behind being beings best better between big both but "
			+ "by c came can cannot case cases certain certainly clear clearly com come "
			+ "could d did differ different differently do does done down downed "
			+ "downing downs during e each early either end ended ending ends enough "
			+ "even evenly ever every everybody everyone everything everywhere f "
			+ "fact facts far felt few find finds for from full fully further furthered "
			+ "furthering furthers g gave general generally get gets give given gives go going "
			+ "goof goods got great greater greatest group grouped grouping groups h had has have "
			+ "having he her here herself high higher highest him himself his how however i if "
			+ "important in interest interested interesting interests into is it its itself "
			+ "j just k keep keeps kind kind knew know known l large largely last later latest least "
			+ "less let lets like likely long longer longest m made make making man many may me member "
			+ "members men might more most mostly mr mrs much must my myself n necessary need needed needing "
			+ "needs never new newer newest next no nobody non noone not nothing now nowhere number numbers "
			+ "o of off often on once one only open opened opening opens or order ordered "
			+ "ordering orders other others our out over p part parted parting parts per pre perhaps place places "
			+ "point pointed pointing points present presented presenting presents problem problems put "
			+ "puts q quite r rather really right room rooms s said same saw say says second see "
			+ "seem seemed seemingseems sees several shall she should show showed showing shows side sides "
			+ "since small smaller smallest so some somebody someone something somewhere state states "
			+ "still such sure t take taken than that the their them then there therefore these they "
			+ "thing things think thinks this those though thought thoughts three through thus to today "
			+ "together too took toward turn turned turning turns two u under until up upon "
			+ "us use used uses v very w want wanting wants was way ways we well wells went were "
			+ "what when where whether which while who whole whose why will within without "
			+ "work worked working works would x y year years yet young you younger youngest your yours z"
			+ "online welcome with specific site";

	private static Set<String> stopWords;
	static {
		stopWords = new HashSet<String>();
		stopWords.addAll(Arrays.asList(STOP_WORDS.split(" ")));
	}

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
		this.databaseURL = url;
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

		HashSet<String> categories = new HashSet<String>();
		categories.add("queries/Root.txt");
		int level = 1;
		System.out.println("Classifying ... \n\n");
		do {
			// for storing child categories while iterating
			HashSet<String> tempCats = new HashSet<String>();
			for (String cat : categories) {
				String catName = cat.split("queries\\/|.txt")[1];
				System.err.println(cat);
				Set<String> docs = new HashSet<String>();
				cachedResults.put(catName, docs);
				int coverage = 0;
				try {
					FileInputStream fstream = new FileInputStream(cat);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(in));
					String query;
					int cCoverage = 0;
					String previousCategory = null;
					while ((query = br.readLine()) != null) {
						String[] queryTerms = query.split(" ");
						if ((previousCategory != null)
								&& (!queryTerms[0].equals(previousCategory))) {
							this.catCoverage.put(previousCategory, cCoverage);
							cCoverage = 0;
						}
						tempCats.add("queries/" + queryTerms[0] + ".txt");
						int numhits = this.poseQuery(queryTerms, docs);
						// Try to avoid abusing the site
						// Thread.sleep(5000);
						coverage += numhits;
						cCoverage += numhits;
						previousCategory = queryTerms[0];
					}
					this.catCoverage.put(previousCategory, cCoverage);
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

			// remove all categories which are < Ts or Tc
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

		/* classified categories */
		for (String fc : this.catSpecificity.keySet()) {
			System.err.println("sc " + fc + " " + this.catSpecificity.get(fc)
					+ " " + this.catCoverage.get(fc));
		}

		// build content summary
		this.buildContentSummary();
		return null;
	}

	public void buildContentSummary() {

		/* document sampling after category selection */
		List<DocumentSample> docSample = new ArrayList<DocumentSample>();
		DocumentSample dc;
		List<String> childList = new ArrayList<String>();
		// classified category will always be some leaf
		for (String fc : this.catSpecificity.keySet()) {
			dc = new DocumentSample();
			// category for this document sample
			dc.category = fc;
			// all the documents related to this category
			dc.filteredURLs = cachedResults.get(fc);
			docSample.add(dc);
			String tempCat = fc;
			String parent = null;
			while ((parent = this.getParent(tempCat)) != null) {
				childList.add(tempCat);
				dc = new DocumentSample();
				dc.category = parent;
				dc.filteredURLs = cachedResults.get(parent);
				// add all the child documents
				for (String c : childList) {
					dc.filteredURLs.addAll(cachedResults.get(c));
				}
				// add to the document class list
				docSample.add(dc);
				tempCat = parent;
			}
			// prepare for the next sample
			childList.clear();
		}

		// fetch all the webpages for each doc sample
		for (DocumentSample ds : docSample) {
			int docId = 0;
			System.out.println("Building document sample for " + ds.category);
			for (String url : ds.filteredURLs) {
				System.out.printf("[%d] Getting page: %s \n",(docId+1),url);
				Set<String> words = URLProcessor.runLynx(url);
				Document d = new Document(url);
				d.words = words;
				ds.urlMap.put(url, d);
				ds.wordSet.addAll(words);
				docId++;
			}

			String fileName = "sample-"+ds.category+"-"+this.databaseURL+".txt";
			// display result
			this.display(ds, fileName);
		}
		System.out.println("Finished generating document samples..");
	}

	/*
	 * Display the word-document matrix
	 */
	public void display(DocumentSample ds, String fileName) {
		FileWriter fstream;
		BufferedWriter out;
		try {
			fstream = new FileWriter(fileName, true);
			out = new BufferedWriter(fstream);
			out.write("Document Sample for " + ds.category + "\n");
			out.write("##############################\n");
			int docCount = 0;
			for (String word : ds.wordSet) {
				out.write(word + " - ");
				for (String url : ds.filteredURLs) {
					Document d = ds.urlMap.get(url);
					if(d.words.contains(word))
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
	 * 
	 * @param queryTerms
	 * @param categoryName
	 * @return
	 */
	public int poseQuery(String[] queryTerms, Set<String> docs) {

		String query = new String();
		JSONArray ja = new JSONArray();
		// Convert spaces to + to make a valid URL
		try {
			for (int i = 1; i < queryTerms.length; i++)
				query += " " + queryTerms[i];
			query = URLEncoder.encode(query, "UTF-8");
			//System.out.println("Probing Query: " + query);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		JSONObject j = null;
		URL url;
		try {
			url = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + query
					+ "?appid=" + API_KEY + "&sites=" + this.databaseURL + "&start=0"
					+ "&count=" + YahooProber.topResults + "&format=json");
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

			for (int i = 0; i < YahooProber.topResults; i++) {
				if (ja.length() == i) {
					break;
				}
				// System.out.print((i + 1) + ". ");
				j = ja.getJSONObject(i);
				// System.out.println(j.getString("url"));
				docs.add(j.getString("url"));

			}
			// System.err.println("Number of matches :" + query + " ="+ numHits);
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
