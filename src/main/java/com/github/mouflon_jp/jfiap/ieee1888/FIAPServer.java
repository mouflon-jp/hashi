package com.github.mouflon_jp.jfiap.ieee1888;

import javax.jws.WebService;

import org.fiap.soap.DataRQ;
import org.fiap.soap.DataRS;
import org.fiap.soap.FIAPServiceSoap;
import org.fiap.soap.QueryRQ;
import org.fiap.soap.QueryRS;

@WebService(endpointInterface = "org.fiap.soap.FIAPServiceSoap")
public class FIAPServer implements FIAPServiceSoap{

	@Override
	public QueryRS query(QueryRQ parameters) {
		return null;
	}

	@Override
	public DataRS data(DataRQ parameters) {
		return null;
	}

}
