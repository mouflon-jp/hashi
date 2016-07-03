package com.github.mouflon_jp.jfiap.ieee1888;

import java.time.Instant;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.BindingProvider;

import org.fiap.soap.DataRQ;
import org.fiap.soap.DataRS;
import org.fiap.soap.FIAPServiceSoap;
import org.fiap.soap.FIAPStorage;
import org.fiap.soap.QueryRQ;
import org.fiap.soap.QueryRS;

import jp.gutp.fiap._2009._11.Body;
import jp.gutp.fiap._2009._11.Header;
import jp.gutp.fiap._2009._11.Key;
import jp.gutp.fiap._2009._11.Point;
import jp.gutp.fiap._2009._11.PointSet;
import jp.gutp.fiap._2009._11.Query;
import jp.gutp.fiap._2009._11.QueryType;
import jp.gutp.fiap._2009._11.Transport;
import jp.gutp.fiap._2009._11.Value;

public class FIAPClient {
	private ThreadLocal<DatatypeFactory> dtf = new ThreadLocal<DatatypeFactory>(){
		@Override
		protected DatatypeFactory initialValue() {
			try {
				return DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new InternalError(e);
			}
		};
	};
	private FIAPServiceSoap __fiapServiceSoap;


	/**
	 * Constructor
	 *
	 * @param ep IEEE1888 Storage Endpoint Address
	 */
	public FIAPClient(String ep) {
		__fiapServiceSoap = new FIAPStorage().getFIAPServiceSoap();
		BindingProvider bindingProvider = (BindingProvider)__fiapServiceSoap;
		bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ep);
	}

	/**
	 * Query Web Method
	 *
	 * @param queryRQ Request Message
	 * @return Response Message
	 */
	public QueryRS query(QueryRQ queryRQ){
		return __fiapServiceSoap.query(queryRQ);
	}

	/**
	 * Data Web Method
	 *
	 * @param dataRQ Request Message
	 * @return Response Message
	 */
	public DataRS data(DataRQ dataRQ){
		return __fiapServiceSoap.data(dataRQ);
	}

	/**
	 * Query Web Method
	 *
	 * @param transport Request Message
	 * @return Response Message
	 * @throws FIAPException
	 */
	public Transport query(Transport transport) throws FIAPException {
		QueryRQ queryRQ = new QueryRQ();
		queryRQ.setTransport(transport);
		QueryRS queryRS = query(queryRQ);
		if(queryRS.getTransport() == null){
			throw new FIAPException("No Transport");
		}
		return queryRS.getTransport();
	}

	/**
	 * Data Web Method
	 *
	 * @param trasnport Request Message
	 * @return Response Message
	 * @throws FIAPException
	 */
	public Transport data(Transport trasnport) throws FIAPException {
		DataRQ dataRQ = new DataRQ();
		dataRQ.setTransport(trasnport);
		DataRS dataRS = __fiapServiceSoap.data(dataRQ);
		if(dataRS.getTransport() == null){
			throw new FIAPException("No Transport");
		}
		return dataRS.getTransport();
	}

	public Body fetch(Query query) throws FIAPException{
		Body retBody = new Body();

		while(true){
			Header header = new Header();
			header.setQuery(query);

			Transport req = new Transport();
			req.setHeader(header);

			Transport res = query(req);
			validateTransport(res);

			Body body = res.getBody();
			if(body==null){
				throw new FIAPException("No Body");
			}

			for(PointSet ps : body.getPointSet()){
				Optional<PointSet> mps =
						retBody.getPointSet().stream()
						.filter(it -> it.getId().equals(ps.getId()))
						.findFirst();

				if(mps.isPresent()){
					PointSet eps = mps.get();
					eps.getPoint().addAll(ps.getPoint());
					eps.getPointSet().addAll(ps.getPointSet());

				} else {
					retBody.getPointSet().add(ps);

				}
			}

			for(Point p: body.getPoint()){
				Optional<Point> mp =
						retBody.getPoint().stream()
						.filter(it -> it.getId().equals(p.getId()))
						.findFirst();

				if(mp.isPresent()){
					Point ep = mp.get();
					ep.getValue().addAll(p.getValue());

				} else {
					retBody.getPoint().add(p);
				}
			}

			query = res.getHeader().getQuery();
			if(query==null || query.getCursor()==null){
				break;
			}
		}

		return retBody;
	}

	public Map<String, Map<Instant, String>> fetchPoint(List<Key> keys) throws FIAPException{
		Query query = new Query();
		query.setId(UUID.randomUUID().toString());
		query.setType(QueryType.STORAGE);
		Body body = fetch(query);
		List<Point> points = body.getPoint();

		Map<String, Map<Instant, String>> ret = new HashMap<>();
		for (Point point : points) {
			if(!ret.containsKey(point.getId())){
				ret.put(point.getId(), new HashMap<>());
			}

			Map<Instant, String> values = ret.get(point.getId());
			values.putAll(
				point.getValue().stream()
				.collect(Collectors.toMap(
						it -> it.getTime().toGregorianCalendar().toInstant(),
						it -> it.getValue()
				))
			);
		};
		return ret;
	}

	public void write(Body body) throws FIAPException{
		Transport req = new Transport();
		req.setBody(body);
		data(req);
	}

	public void writePoint(Map<String, Map<Instant, String>> pointValues) throws FIAPException{
		Body body = new Body();
		DatatypeFactory dataFactory = dtf.get();
		GregorianCalendar cal = new GregorianCalendar();

		for (Entry<String, Map<Instant, String>> pointValue : pointValues.entrySet()) {
			Point point = new Point();
			point.setId(pointValue.getKey());

			point.getValue().addAll(
				pointValue.getValue().entrySet().stream()
				.map(it -> {
					Value value = new Value();
					cal.setTimeInMillis(it.getKey().toEpochMilli());
					value.setTime(dataFactory.newXMLGregorianCalendar(cal));
					value.setValue(it.getValue());
					return value;
				})
				.collect(Collectors.toList())
			);

			body.getPoint().add(point);
		}
	}

	private void validateTransport(Transport trans) throws FIAPException {
		Header h = trans.getHeader();
		if(h == null){
			throw new FIAPException("No Header");
		}

		if(h.getOK()==null){
			throw new FIAPException("No OK");
		}

		jp.gutp.fiap._2009._11.Error e = h.getError();
		if(e!=null){
			throw new FIAPException("[" + e.getType() + "] " + e.getValue());
		}

	}

}
