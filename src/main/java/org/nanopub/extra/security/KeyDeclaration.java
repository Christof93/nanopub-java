package org.nanopub.extra.security;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class KeyDeclaration extends CryptoElement {

	private static final long serialVersionUID = 1L;

	public static final IRI DECLARED_BY = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/declaredBy");
	public static final IRI HAS_KEY_LOCATION = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/hasKeyLocation");

	// TODO: Shouldn't there be only one declarer?
	private Set<IRI> declarers = new LinkedHashSet<>();
	private IRI keyLocation;

	KeyDeclaration(IRI uri) {
		super(uri);
	}

	void addDeclarer(IRI declarer) {
		declarers.add(declarer);
	}

	public Set<IRI> getDeclarers() {
		return declarers;
	}

	public boolean hasDeclarer(IRI declarer) {
		return declarers.contains(declarer);
	}

	void setKeyLocation(IRI keyLocation) {
		this.keyLocation = keyLocation;
	}

	public IRI getKeyLocation() {
		return keyLocation;
	}

}
