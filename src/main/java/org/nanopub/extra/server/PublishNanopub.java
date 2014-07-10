package org.nanopub.extra.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.rio.RDFFormat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class PublishNanopub {

	@com.beust.jcommander.Parameter(description = "nanopubs", required = true)
	private List<String> nanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	@com.beust.jcommander.Parameter(names = "-u", description = "Use the given nanopub server URLs")
	private List<String> serverUrls;

	@com.beust.jcommander.Parameter(names = "-s", description = "Get nanopubs to be published from given SPARQL endpoint")
	private String sparqlEndpointUrl;

	public static void main(String[] args) {
		PublishNanopub obj = new PublishNanopub();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void publish(Nanopub nanopub) throws IOException {
		new PublishNanopub().publishNanopub(nanopub);
	}

	private ServerIterator serverIterator = null;
	private String serverUrl = null;
	private List<String> usedServers = new ArrayList<>();
	private int count, published;
	private boolean failed;
	private SPARQLRepository sparqlRepo;

	public PublishNanopub() {
	}

	private void run() throws IOException {
		published = 0;
		failed = false;
		for (String s : nanopubs) {
			count = 0;
			try {
				if (sparqlEndpointUrl != null) {
					if (sparqlRepo == null) {
						sparqlRepo = new SPARQLRepository(sparqlEndpointUrl);
						sparqlRepo.initialize();
					}
					Nanopub np = new NanopubImpl(sparqlRepo, new URIImpl(s));
					try {
						publishNanopub(np);
						published++;
					} catch (IOException ex) {
						failed = true;
					}
				} else {
					if (verbose) {
						System.out.println("Reading file: " + s);
					}
					MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
						@Override
						public void handleNanopub(Nanopub np) {
							count++;
							if (failed) return;
							try {
								publishNanopub(np);
								published++;
							} catch (IOException ex) {
								failed = true;
							}
						}
					});
					if (count == 0) {
						System.out.println("NO NANOPUB FOUND: " + s);
						break;
					}
				}
			} catch (OpenRDFException ex) {
				System.out.println("RDF ERROR: " + s);
				ex.printStackTrace(System.err);
				break;
			} catch (MalformedNanopubException ex) {
				System.out.println("INVALID NANOPUB: " + s);
				ex.printStackTrace(System.err);
				break;
			}
			if (failed) {
				System.out.println("FAILED TO PUBLISH NANOPUBS");
				break;
			}
		}
		System.out.println(published + " nanopubs published at:");
		for (String s : usedServers) {
			System.out.println("- " + s);
		}
		if (sparqlRepo != null) {
			try {
				sparqlRepo.shutDown();
			} catch (RepositoryException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void publishNanopub(Nanopub nanopub) throws IOException {
		if (serverUrl == null) {
			if (serverUrls == null || serverUrls.isEmpty()) {
				serverIterator = new ServerIterator();
			} else {
				serverIterator = new ServerIterator(serverUrls);
			}
			serverUrl = serverIterator.next();
		}
		while (serverUrl != null) {
			if (!ServerInfo.load(serverUrl).isPostNanopubsEnabled()) {
				serverUrl = serverIterator.next();
				continue;
			}
			try {
				HttpPost post = new HttpPost(serverUrl);
				String nanopubString = NanopubUtils.writeToString(nanopub, RDFFormat.TRIG);
				post.setEntity(new StringEntity(nanopubString));
				post.setHeader("Content-Type", RDFFormat.TRIG.getDefaultMIMEType());
				HttpResponse response = HttpClientBuilder.create().build().execute(post);
				int code = response.getStatusLine().getStatusCode();
				if (code >= 200 && code < 300) {
					if (!usedServers.contains(serverUrl)) {
						usedServers.add(serverUrl);
					}
					if (verbose) {
						System.out.println("Published: " + TrustyUriUtils.getArtifactCode(nanopub.getUri().toString()));
					}
					return;
				}
			} catch (IOException ex) {
				// ignore
			} catch (OpenRDFException ex) {
				// ignore
			}
			serverUrl = serverIterator.next();
		}
		throw new IOException("Failed to publish the nanopub");
	}

}
