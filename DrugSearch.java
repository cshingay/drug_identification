package HackThe6;
/* Websites used
 * 		Search up drug name from DIN: https://health-products.canada.ca/dpd-bdpp/index-eng.jsp
 * 		Complications: https://www.drugs.com/
 */
import java.io.IOException;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.Document; //02335948
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

public class DrugSearch {
	public static void main(String [] args) {
		System.out.println("Running...\n");
		
		String din = "02368072"; //dextromethorpan -- 02252554; tylenol -- 00559407; ibuprofen -- 02368072; benylin - 02265427
		@SuppressWarnings("unchecked")
		List<String>[] nameIngr = new LinkedList[2];
		nameIngr[0] = new LinkedList<String>();
		nameIngr[1] = new LinkedList<String>();
		//nameIngr[1] = activeIngredients linkedList
		
		nameIngr = drugIdentification(din);
		
		@SuppressWarnings("unchecked")
		List<String>[] conflicts = new LinkedList[nameIngr[1].size()];
		for(int j = 0; j<nameIngr[1].size(); j++) {
			conflicts[j] = new LinkedList<String>();
		} 
		
		System.out.println("Drug name:\n+ "+nameIngr[0].get(0));
		System.out.println("Active Ingredients:");
		for(int i = 0; i< nameIngr[1].size(); i++) {
			System.out.println("+ "+nameIngr[1].get(i));
		}
		
		LinkedList<String> medList = new LinkedList<String>();
		medList.add("Acetaminophen");

		medList.add("Warfarin");
		
		conflicts = getConflictInfo(nameIngr[1], medList);
		boolean works = true;
		/*for(int i = 0; i< conflicts.length; i++) {
			for(int k = 0; k<medList.size(); k++) {
				try {
					System.out.println(conflicts[i].get(k));
				}
				catch(IndexOutOfBoundsException ee) {
					works = false;
					break;
				}
			}
		}
		if(works) {*/
			for(int i = 0; i< conflicts.length; i++) {
				System.out.println();
				System.out.println("Active Ing: "+nameIngr[1].get(i));
				for(int k = 0; k<medList.size(); k++) {
					try {
						System.out.println("Med: "+medList.get(k));
						System.out.println(conflicts[i].get(k));
					}
					catch(IndexOutOfBoundsException ee) {
						break;
					}
				}
			}
		//}
			
		System.out.println("\n\nDone.");
		
	}
	
	/****************************************
	 * 
	 * Takes given DIN and will search in canada drug site
	 * Returns array of lists
	 * 		 • first index is drug name
	 * 		 • second index is active ingredients
	 * 
	 ****************************************/
	
	@SuppressWarnings("unchecked")
	public static List<String>[] drugIdentification(String din) { //FOR DIN NUMBER
		final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
		List<String>[] returnVals = new LinkedList[2];
		returnVals[0] = new LinkedList<String>();
		returnVals[1] = new LinkedList<String>();
		
		try {			
			String dinURL  = "https://health-products.canada.ca/dpd-bdpp/index-eng.jsp";
			Connection.Response dinResp = Jsoup.connect(dinURL)
											.method(Connection.Method.GET)
											.userAgent(USER_AGENT)
											.execute(); 
			
			FormElement searchForm = (FormElement) dinResp.parse().select("form").get(1);
			Element dinField = searchForm.select("#din").first();
			dinField.val(din);
			
			Connection.Response searchActionResponse = searchForm.submit()
																.cookies(dinResp.cookies())
																.userAgent(USER_AGENT)
																.execute();
	
			//drugDinDoc == drug page associated with given DIN
			//Now searching for drug name within page
			
			Document drugDinDoc = searchActionResponse.parse();
			Element drug = drugDinDoc.select("main, div.row").get(7);
			
			//Getting drug name
			
			String drugName = drug.select("p.col-sm-8").text();
		//	System.out.println();
		//	System.out.println("Product Name: " + drugName);
		//	System.out.println();
			returnVals[0].add(drugName);
		
			//Getting active ingredients
			
			Elements active = drugDinDoc.selectFirst("main, div.table-responsive mrgn-tp-lg").getElementsByTag("td");
			int size = active.size();
			int i = 0;
			List<String>[] activeIng = new LinkedList[2];
			activeIng[0] = new LinkedList<String>();
			activeIng[1] = new LinkedList<String>();
			
			while(i<size) {
				activeIng[0].add(active.get(i).text());
				returnVals[1].add(active.get(i).text());
				i++;
				activeIng[1].add(active.get(i).text());
				i++;
			}
			
		//	System.out.println("Active Ing \t\t\t\tStrength");
		//	for(i = 0; i<activeIng[0].size(); i++) {
		//		System.out.println(activeIng[0].get(i) + "\t\t\t\t" + activeIng[1].get(i));
		//	}						
		} 		
		catch (IOException e) {
			e.printStackTrace();
		}
		return returnVals;
	}

	/****************************************
	 * 
	 * Takes linkedlist of activeIngr. & medList; searches for conflicts in drugs
	 * Returns array of lists
	 * 		 • first index is interactions with active ing. 1 and med list
	 * 		 • second index is interactions with active ing. 2 and med list 
	 * 		 • etc. 
	 * 
	 ****************************************/
	
	public static List<String>[] getConflictInfo(List<String> activeIng, LinkedList<String> medList){ //PLS RUN ONLY IF HAVE MEDS
		final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
		List<String> [] returnVals = new LinkedList[activeIng.size()];
		
		for(int kl = 0; kl<activeIng.size(); kl++) {
			returnVals[kl] = new LinkedList<String>();
		}
		
		List<String> interactionCodes = new LinkedList<String>();
		List<String> activeIngCodes = new LinkedList<String>();
		List<String> renamed = new LinkedList<String>(); //substring list of active ingredients
		List<String> conflictInfo = new LinkedList<String>();
		int i;
		
		try {

			/****************************************
			 * 
			 * LOOKING FOR DRUG INTERACTIONS
			 * 
			 ****************************************/
			//needs activeIng
			for(i = 0; i<activeIng.size(); i++) {
				if(activeIng.get(i).contains(" ")) {
					renamed.add(activeIng.get(i).substring(0, activeIng.get(i).indexOf(" ")));
				}
				else {
					renamed.add(activeIng.get(i));
				}
			}
			
			/*****************************************
			 * 
			 * EXTRACTS ID FROM EACH INGREDIENT
			 * renamed -- ingrList
			 * activeIngCodes -- id codes
			 * ingrFoodInteractions -- for the messages
			 * 
			 *****************************************/
			
			String interactionsURL = "https://www.drugs.com/"+renamed.get(0)+".html";
			Connection.Response interactPage = Jsoup.connect(interactionsURL)
					.method(Connection.Method.GET)
					.userAgent(USER_AGENT)
					.execute();
			interactPage.bufferUp();
			Document interactDoc = interactPage.parse();
			Element idNums = interactDoc.getElementById("pronounce-audio").selectFirst("source");
			//checkElem("idnums", idNums);
			String ogDrugID = idNums.attributes().html();
			ogDrugID = ogDrugID.substring(17, ogDrugID.indexOf("."));
			activeIngCodes.add(ogDrugID);
			
			for(i=1; i<renamed.size(); i++) {
				interactionsURL = "https://www.drugs.com/"+renamed.get(i)+".html";
				interactPage = Jsoup.connect(interactionsURL)
						.method(Connection.Method.GET)
						.userAgent(USER_AGENT)
						.execute();
				interactDoc = interactPage.parse();
				idNums = interactDoc.getElementById("pronounce-audio").selectFirst("source");
				//checkElem("idnums", idNums);
				ogDrugID = idNums.attributes().html();
				ogDrugID = ogDrugID.substring(17, ogDrugID.indexOf("."));
				activeIngCodes.add(ogDrugID);
			}	
			
			/************************
			 *
			 * GETTING MEDLIST CODES
			 * 
			 * renamed -- ingrList
			 * activeIngCodes -- id codes
			 * ingrFoodInteractions -- for the messages
			 * 
			 ************************/
			
			Connection.Response conflictPage = null;
			String conflictURL;
			Document conflictDoc;
			Element newIDNums;
			String newDrugID;
			boolean linkFound = true;
			
			//getting medlist drug codes
			//alteration to add: search up medication name, get active ingredient
			
			if(medList.size()>0) {
				try {
					conflictURL = "https://www.drugs.com/"+medList.get(0)+".html";
					conflictPage = Jsoup.connect(conflictURL)
							.method(Connection.Method.GET)
							.userAgent(USER_AGENT)
							.execute();
				}
				catch (HttpStatusException excep) {
					System.out.println("Medication list spelt incorrectly or unable to find. Recheck and try again!");
					linkFound = false;
					return returnVals;
				}
				if(linkFound) {
					conflictPage.bufferUp();
					conflictDoc = conflictPage.parse();
					newIDNums = conflictDoc.getElementById("pronounce-audio").selectFirst("source");
					//checkElem("idnums", newIDNums);
					newDrugID = newIDNums.attributes().html();
					newDrugID = newDrugID.substring(17, newDrugID.indexOf("."));
					interactionCodes.add(newDrugID);
				}
			}
			
			for(int k=1; k<medList.size(); k++) {		
				try {
					conflictURL = "https://www.drugs.com/"+medList.get(k)+".html";
					conflictPage = Jsoup.connect(conflictURL)
							.method(Connection.Method.GET)
							.userAgent(USER_AGENT)
							.execute();
				}
				catch (HttpStatusException excep) {
					System.out.println("Medication list spelt incorrectly or unable to find. Recheck and try again!");
					linkFound = false;
					return returnVals;
				}
				
				if(linkFound) {
					conflictDoc = conflictPage.parse();
					newIDNums = conflictDoc.getElementById("pronounce-audio").selectFirst("source");
					//checkElem("idnums", newIDNums);
					newDrugID = newIDNums.attributes().html();
					newDrugID = newDrugID.substring(17, newDrugID.indexOf("."));
					interactionCodes.add(newDrugID);			
				}
			}
			
			/**********************
			 * 
			 * interactionCodes: LinkedList<String> -- medication
			 * activeIngCodes: LinkedList<String> -- ingredients
			 * conflictInfo: LinkedList<String>
			 * ogDrugID: string
			 * Now populating conflictInfo linked list
			 * 
			 **********************/
			
			if(medList.size() > 0) {
				String finalURL = "https://www.drugs.com/interactions-check.php?drug_list="+activeIngCodes.get(0)+"%2C"+interactionCodes.get(0)+"&interaction_list_id=0&professional=0&types%5B%5D=major&types%5B%5D=moderate&types%5B%5D=minor";
				
				Connection.Response finalPage = Jsoup.connect(finalURL)
						.method(Connection.Method.GET)
						.userAgent(USER_AGENT)
						.execute();
				
				finalPage.bufferUp();
				Document finDoc = finalPage.parse();
				Element para;
				Element selectFrom;
				Element warningLevel;
				String warning;
				int index;
				String info;
				
				LinkedList<String> indexI = new LinkedList<String>();
				for(i=0; i<activeIngCodes.size(); i++) {
					indexI.clear();
					for(int j=0; j<interactionCodes.size(); j++) {
						finalURL = "https://www.drugs.com/interactions-check.php?drug_list="+activeIngCodes.get(0)+"%2C"+interactionCodes.get(0)+"&interaction_list_id=0&professional=0&types%5B%5D=major&types%5B%5D=moderate&types%5B%5D=minor";
						
						finalPage = Jsoup.connect(finalURL)
								.method(Connection.Method.GET)
								.userAgent(USER_AGENT)
								.execute();
						
						finDoc = finalPage.parse();
//						System.out.println(finDoc.location());

						try {
							para = finDoc.selectFirst(".interactions-reference");
							selectFrom = para.select("p").get(1);
							info = selectFrom.text();
							warning = para.text();
							index = warning.indexOf(" ");
							warning = warning.substring(0, index);
							if(warning.contains("Minor")) {
								info = "Minor: Consumer information is not available. "
										+ "Minor interactions usually do not cause harm, "
										+ "however, your health care provider can give you more information.";
							}
							else
								info = warning + ": " + info;
						}
						catch (NullPointerException exception){
							info = "No Relevant Conflicts";
						}
						indexI.add(info);
					}
					returnVals[i] = indexI;
				}
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}	
		
		return returnVals;
		//return (LinkedList) conflictInfo;
	}
}