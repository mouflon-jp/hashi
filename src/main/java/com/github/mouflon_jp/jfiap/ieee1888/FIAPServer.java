package com.github.mouflon_jp.jfiap.ieee1888;

import javax.jws.WebService;

import org.fiap.soap.DataRQ;
import org.fiap.soap.DataRS;
import org.fiap.soap.FIAPServiceSoap;
import org.fiap.soap.QueryRQ;
import org.fiap.soap.QueryRS;

import jp.gutp.fiap._2009._11.Header;
import jp.gutp.fiap._2009._11.Transport;

@WebService(endpointInterface = "org.fiap.soap.FIAPServiceSoap")
public class FIAPServer implements FIAPServiceSoap{

	public FIAPServer(FIAPServerHandler handler) {
		this((param) -> handler.query(param), (param) -> handler.data(param));
	}

	public FIAPServer(FIAPServerMethodHandler queryHandler, FIAPServerMethodHandler dataHandler) {
		this.queryHandler = queryHandler;
		this.dataHandler = dataHandler;
	}

	private FIAPServerMethodHandler queryHandler;
	private FIAPServerMethodHandler dataHandler;

	@Override
	public QueryRS query(QueryRQ queryRQ) {
		Transport req = queryRQ.getTransport();
		Transport res = call(queryHandler, req);
		QueryRS queryRS = new QueryRS();
		queryRS.setTransport(res);
		return queryRS;
	}

	@Override
	public DataRS data(DataRQ dataRQ) {
		Transport req = dataRQ.getTransport();
		Transport res = call(dataHandler, req);
		DataRS dataRS = new DataRS();
		dataRS.setTransport(res);
		return dataRS;
	}

	private static Transport call(FIAPServerMethodHandler handler, Transport  req){
		Transport res;
		try {
			if(req == null){
				throw new FIAPException("No Transport");
			}
			res = handler.call(req);

		} catch (Throwable e){
			res = errorTransport(req, e.getClass().getName(), e.getLocalizedMessage());
		}
		return res;
	}


	public static Transport errorTransport(Transport req, String type, String value){
		jp.gutp.fiap._2009._11.Error errorPayload = new jp.gutp.fiap._2009._11.Error();
		errorPayload.setType(type);
		errorPayload.setValue(value);

		Transport trans = req;
		if(trans == null){
			trans = new Transport();
		}

		Header header = trans.getHeader();
		if(header == null){
			header = new Header();
		}

		header.setError(errorPayload);
		trans.setHeader(header);

		return trans;
	}

	public static Transport okTransport(Transport req){
		jp.gutp.fiap._2009._11.OK okPayload = new jp.gutp.fiap._2009._11.OK();

		Transport trans = req;
		if(trans == null){
			trans = new Transport();
		}

		Header header = trans.getHeader();
		if(header == null){
			header = new Header();
		}

		header.setOK(okPayload);
		trans.setHeader(header);
		return trans;
	}

	@FunctionalInterface
	public static interface FIAPServerMethodHandler {
		public Transport call(Transport request) throws Exception;
	}

	public static interface FIAPServerHandler {
		public Transport query(Transport request) throws Exception;
		public Transport data(Transport request) throws Exception;
	}

	public static FIAPServerMethodHandler NOT_IMPLEMENTED_HANDLER = (req) -> {
		throw new UnsupportedOperationException("Not implemented yet");
	};
}
