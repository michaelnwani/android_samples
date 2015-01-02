package com.bignerdranch.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
	private static final String API_KEY = "0cbcb0c378b8d5dd3b43be5b6c0232ed";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flick.photos.search";
	private static final String PARAM_EXTRAS = "extras";
	private static final String PARAM_TEXT = "text";
	
	private static final String EXTRA_SMALL_URL = "url_s";
	
	private static final String XML_PHOTO = "photo";
	
	byte[] getUrlBytes(String urlSpec) throws IOException
	{
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				return null;
			}
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			//reads a byte, returns a number between 0 and 255
			//returns -1 if the end of the stream has been reached, that's why we have "> 0"
			//stores each byte in the designated array (buffer)
			while ((bytesRead = in.read(buffer)) > 0)  
			{
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			return out.toByteArray();
		}
		finally
		{
			connection.disconnect();
		}
	}
	
	public String getUrl(String urlSpec) throws IOException
	{
		return new String(getUrlBytes(urlSpec)); //converts a byte array into a string using a default charset
	}
	
	public ArrayList<GalleryItem> downloadGalleryItems(String url)
	{
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		try
		{
			//returns the bytes from the data at this url, into a string of encoded charsets, which
			//just happens to be in the form of perfect XML
			String xmlString = getUrl(url);
			Log.i(TAG, "Receieved xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItems(items, parser);
		}
		catch (IOException ioe)
		{
			Log.e(TAG, "Failed to fetch items", ioe);
		}
		catch (XmlPullParserException xppe)
		{
			Log.e(TAG, "Failed to parse items", xppe);
		}
		
		return items;
	}
	
	
	public ArrayList<GalleryItem> fetchItems()
	{
		
			String url = Uri.parse(ENDPOINT).buildUpon()
					.appendQueryParameter("method", METHOD_GET_RECENT)
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
					.build().toString();
			return downloadGalleryItems(url);

	}
	
	public ArrayList<GalleryItem> search(String query)
	{
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		return downloadGalleryItems(url);
	}
	
	
	void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException
	{
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName()))
			{
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				String owner = parser.getAttributeValue(null, "owner");
				
				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				item.setOwner(owner);
				items.add(item);
				
				
				
			}
			eventType = parser.next();
		}
	}
}
