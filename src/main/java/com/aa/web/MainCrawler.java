package com.aa.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class MainCrawler {
	private static final int NUMBER_OF_PAGES_TO_VISIT = 1000;
	private static final String ENV = "www.aa.com";
	private static final String HOMEPAGE = "https://www.aa.com/homePage.do";
	int numOfVisits = 0;
	private static String HIDDEN_TEXT = "Opens another site in a new window that may not meet accessibility guidelines";
	private static String HIDDEN_TEXT2 = "Link opens another site that may not meet accessibility guidelines";
	private static String SPA_HIDDEN_TEXT = "Abre una nueva ventana de otro sitio web que podría no cumplir con las normas de accesibilidad";
	private static String CLOSEJEM = "closeJSEM.do";
	private static String LOCALE = "locale";
	private Set<String> pagesVisited = new HashSet<>();
	private Set<String> pages404 = new HashSet<>();	
	private Set<String> pagesWithouti18n = new HashSet<>();
	
	XSSFWorkbook workbook = new XSSFWorkbook(); 
    XSSFSheet spreadsheet = workbook.createSheet("ELM missing URLs");
    int rowNumber = 0;
    
	public static void main(String[] args) {
		MainCrawler crawler = new MainCrawler();
		crawler.processURL(HOMEPAGE);
		
		try {
			crawler.createExcel();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Execution Complete....");
	}

	private void processURL(String url) {
		if(!url.contains(ENV)) {//some pub pages use higher env from lower env
			return;
		}
		if(pagesVisited.contains(url) || pages404.contains(url) || numOfVisits++ > NUMBER_OF_PAGES_TO_VISIT || url.contains(CLOSEJEM) || url.contains(LOCALE)) {
			return;
		}
//		System.out.println(numOfVisits+" : " + url);
		Set<String> internalLinks = new HashSet<>();
		
		Document document = getPageDocument(url);
		if(document == null) {
			return;
		}
		pagesVisited.add(url);

		Elements elements = document.select("a[href]");
		for(Element element : elements){
			String link = element.attr("href");
			if(isInternal(link)) {//internal
				internalLinks.add(element.attr("abs:href"));
//				processURL(element.attr("abs:href"));
			} else { //external
				createRow(url, element, link);
			}
		}
		
		for (String internalUrl : internalLinks) {
			processURL(internalUrl);
		}
	}

	private Document getPageDocument(String url) {
		Document doc = null;
		try {
			Connection connection = Jsoup.connect(url);
			Connection.Response response = connection.execute();
			if (response.statusCode() == 404) {
				pages404.add(url);
			}
	        if (response.statusCode() == 200) {
	            doc = connection.get();
	        }
		} catch (Exception e) {
//			System.out.println("Exception : " + e.getMessage() + url);
		}
		return doc;
	}

	private boolean isInternal(String link) {
		return link.contains("aa.com") || link.contains("i18n");
	}
	
	private void createRow(String url, Element element, String link) {
		if (link.contains("http") && !containsHiddenText(element.text()) && !isEmpty(element.text())) {
			XSSFRow row = spreadsheet.createRow(rowNumber++);
			row.createCell(0).setCellValue(url);
			row.createCell(1).setCellValue(element.text());
		    row.createCell(2).setCellValue(link);
			System.out.println(url + " : " + element.text() +" : "+ link);
			
			if(!url.contains("i18n")) {
				pagesWithouti18n.add(url);
			}
		}
	}

	private boolean isEmpty(String text) {
		return text.equals(null) || text.equals("");
	}

	private boolean containsHiddenText(String text) {
		return text.contains(HIDDEN_TEXT) || text.contains(HIDDEN_TEXT2) || text.contains(SPA_HIDDEN_TEXT);
	}
	
	private void createExcel() throws IOException {

		FileOutputStream out = new FileOutputStream(new File("ELM URLs.xlsx"));

		workbook.write(out);
		out.close();
		
		for(String pageWithouti18n : pagesWithouti18n) {
			System.out.println(pageWithouti18n);
		}
	}
}
