package com.lg;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
//import org.elasticsearch.transport.client.PreBuiltTransportClient;
//import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONObject;

public class FetchData {

	public static int fileIncrementor = 0;

	public static void main(String[] args) throws Exception {
		FetchData getDataFromElasticSearch = new FetchData();
		getDataFromElasticSearch.dumpData(args[0],args[1], args[2], Integer.valueOf(args[3]), args[4],args[5],args[6],Integer.valueOf(args[7]));
	}
	/*
	 * index
	 * type
	 * node
	 * port
	 * fromDate
	 * toDate
	 * client
	 * batchSize
	 * 
	 */
	public void dumpData(String index,String type,String node, int port, String fromDate, String toDate,String client,int batchSize) {
		//SearchResponse response = transportClient(node, port).prepareSearch().setScroll(new TimeValue(60000))
		/*SearchResponse response = transportClient(node, port).prepareSearch("perm_doc_1_26").setTypes("Document").setScroll(new TimeValue(60000))	
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setQuery(QueryBuilders.multiMatchQuery(queryString, "_all"))
				//.setPostFilter(QueryBuilders.rangeQuery("createdOn").gte(fromDate).lte(toDate).timeZone("+00:00")) // Filter
				.setSize(10000).execute().actionGet(); */
		try{
			TransportClient transportClient = transportClient(node, port);
			SearchResponse response = transportClient.prepareSearch(index).setTypes(type)
					.setScroll(new TimeValue(60000)).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(QueryBuilders.termQuery("client",client)) // Query
					.setPostFilter(QueryBuilders.rangeQuery("createdOn").gte(fromDate).lte(toDate)) // Filter
					.setSize(batchSize).execute().actionGet();
			while (true) {
				System.out.println("Size : " + response.getHits().getTotalHits());
				if (response.getHits().getHits().length == 0) {
					break;
				}
				writeToAFile(index,type,response);	
				// prepare next query
				response = transportClient.prepareSearchScroll(response.getScrollId())
						.setScroll(new TimeValue(60000)).execute().actionGet();
			}
			
		}catch(Exception exception){
			exception.printStackTrace();
			System.out.println("Exception : "+exception.getMessage()); 
		}
		

	}

	@SuppressWarnings("resource")
	public static TransportClient transportClient(String node, int port) throws UnknownHostException {

		/*return new PreBuiltTransportClient(
				Settings.builder().put(ClusterName.CLUSTER_NAME_SETTING.getKey(), "CERT_REPOSITORY")

						.build())

								.addTransportAddress(new InetSocketTransportAddress(
										InetAddress.getByName(node), port));*/
		
		  Settings settings = Settings.settingsBuilder() .put("cluster.name",
		  "PROD_REPOSITORY").build(); TransportClient client =
		  TransportClient.builder().settings(settings).build().
		  addTransportAddress(new
		 InetSocketTransportAddress(InetAddress.getByName(node), port));
		  return client;
		
	}
	
	public void writeToAFile(String index,String type,SearchResponse response){
		fileIncrementor += 1;
		try {
		File file = new File(index+"."+type+"."+ fileIncrementor + ".json");
		FileWriter writer = new FileWriter(file);
		for (SearchHit hit : response.getHits().getHits()) {
			JSONObject value = new JSONObject(hit.getSource());
			writer.write(value.toString());	
		}
		writer.close();	
		} catch (Exception e) {
			System.out.println("Exception while writing to a file : "+e.getMessage());
			e.printStackTrace();
		}
	}
}
