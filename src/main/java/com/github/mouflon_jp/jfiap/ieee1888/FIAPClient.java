package com.github.mouflon_jp.jfiap.ieee1888;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

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

	// ------------------------------------------------------------------------

	private FIAPServiceSoap __fiapServiceSoap;

	// ------------------------------------------------------------------------

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

	// ------------------------------------------------------------------------

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

	/**
	 * Fetch Method
	 *
	 * @param query
	 * @return
	 * @throws FIAPException
	 */
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

			sumAndNormalizeBody(retBody.getPoint(), retBody.getPointSet(), body);

			query = res.getHeader().getQuery();
			if(query==null || query.getCursor()==null){
				break;
			}
		}

		return retBody;
	}

	/**
	 * Fetch Point Method
	 *
	 * @param keys
	 * @return
	 * @throws FIAPException
	 */
	public Map<String, Map<Instant, String>> fetchPoint(List<Key> keys) throws FIAPException{
		Query query = new Query();
		query.setId(UUID.randomUUID().toString());
		query.setType(QueryType.STORAGE);
		Body body = fetch(query);
		return createPointMap(body.getPoint());
	}

	/**
	 * Write Method
	 *
	 * @param body
	 * @throws FIAPException
	 */
	public void write(Body body) throws FIAPException{
		Transport req = new Transport();
		req.setBody(body);
		Transport res = data(req);
		validateTransport(res);
	}

	/**
	 * Write Points Method
	 *
	 * @param pointValues
	 * @throws FIAPException
	 */
	public void writePoint(Map<String, Map<Instant, String>> pointValues) throws FIAPException{
		Body body = new Body();
		body.getPoint().addAll(createPointList(pointValues));
		write(body);
	}

	/**
	 * Trap Method
	 *
	 * @param query
	 * @param timeout
	 * @return
	 * @throws FIAPException
	 */
	public List<Point> trap(Query query, long timeout) throws FIAPException{
		BigInteger ttl = query.getTtl();
		long ttl_sec = ttl.longValueExact();
		String callbackAddr = query.getCallbackData();

		trapQuery(query);
		ScheduledFuture<List<Point>> future = trapData(callbackAddr, ttl_sec);
		try {
			return future.get(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException|ExecutionException|TimeoutException e) {
			throw new FIAPException(e);
		}
	}


	/**
	 * Trap Method
	 *
	 * @param keys
	 * @param callbackAddr
	 * @param ttl_sec
	 * @param timeout
	 * @return
	 * @throws FIAPException
	 */
	public List<Point> trap(List<Key> keys,  String callbackAddr, long ttl_sec, long timeout) throws FIAPException{
		trapQuery(keys, callbackAddr, ttl_sec);
		ScheduledFuture<List<Point>> future = trapData(callbackAddr, ttl_sec);
		try {
			return future.get(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException|ExecutionException|TimeoutException e) {
			throw new FIAPException(e);
		}
	}

	/**
	 * Query of Trap Method
	 *
	 * @param query
	 * @throws FIAPException
	 */
	public void trapQuery(Query query) throws FIAPException{
		Header header = new Header();
		header.setQuery(query);

		Transport req = new Transport();
		req.setHeader(header);

		Transport res = query(req);
		validateTransport(res);
	}

	/**
	 *
	 * Query of Trap Method
	 *
	 * @param keys
	 * @param callbackAddr
	 * @param ttl_sec
	 * @throws FIAPException
	 */
	public void trapQuery(List<Key> keys, String callbackAddr, long ttl_sec) throws FIAPException{
		Query query = new Query();
		query.setId(UUID.randomUUID().toString());
		query.setType(QueryType.STREAM);
		query.setCallbackData(callbackAddr);
		query.setTtl(BigInteger.valueOf(ttl_sec));
		trapQuery(query);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param callbackAddr
	 * @param ttl_sec
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<List<Point>> trapData(String callbackAddr, long ttl_sec) throws FIAPException{
		return trapData(new ArrayList<>(), callbackAddr, ttl_sec);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param callbackAddr
	 * @param ttl
	 * @param ttl_unit
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<List<Point>> trapData(String callbackAddr, long ttl, TimeUnit ttl_unit) throws FIAPException{
		return trapData(new ArrayList<>(), callbackAddr, ttl, ttl_unit);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl_sec
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<List<Point>> trapData(List<Point> store, String callbackAddr, long ttl_sec) throws FIAPException{
		return trapData(store, callbackAddr, ttl_sec, TimeUnit.SECONDS);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl
	 * @param ttl_unit
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<List<Point>> trapData(List<Point> store, String callbackAddr, long ttl, TimeUnit ttl_unit) throws FIAPException{
		FIAPServer server = new FIAPServer(
				FIAPServer.NOT_IMPLEMENTED_HANDLER,
				(req) -> {
					Body b = req.getBody();
					if(b==null){
						throw new FIAPException("No body");
					}
					sumAndNormalizeBody(store, null, b);
					return FIAPServer.okTransport(req);
				}
		);

		ScheduledExecutorService service = getScheduledExecutorService();

		Endpoint endpoint = Endpoint.create(server);
		endpoint.publish(callbackAddr);
		ScheduledFuture<List<Point>> future = service.schedule(
				() -> {
					endpoint.stop();
					return store;
				},
				ttl,
				ttl_unit
		);

		return future;
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl_sec
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<BlockingDeque<Point>> trapData(BlockingDeque<Point> store, String callbackAddr, long ttl_sec) throws FIAPException{
		return trapData(store, callbackAddr, ttl_sec, TimeUnit.SECONDS);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl
	 * @param ttl_unit
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<BlockingDeque<Point>> trapData(BlockingDeque<Point> store, String callbackAddr, long ttl, TimeUnit ttl_unit) throws FIAPException{
		FIAPServer server = new FIAPServer(
				FIAPServer.NOT_IMPLEMENTED_HANDLER,
				(req) -> {
					Body b = req.getBody();
					if(b==null){
						throw new FIAPException("No body");
					}
					store.addAll(b.getPoint());
					return FIAPServer.okTransport(req);
				}
		);

		ScheduledExecutorService service = getScheduledExecutorService();
		Endpoint endpoint = Endpoint.create(server);
		endpoint.publish(callbackAddr);
		ScheduledFuture<BlockingDeque<Point>>  future = service.schedule(
				() -> {
					endpoint.stop();
					return store;
				},
				ttl,
				ttl_unit
		);

		return future;
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl_sec
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<Map<String, Map<Instant, String>>> trapData(Map<String, Map<Instant, String>> store, String callbackAddr, long ttl_sec) throws FIAPException{
		return trapData(store, callbackAddr, ttl_sec, TimeUnit.SECONDS);
	}

	/**
	 *
	 * Data of Trap Method
	 *
	 * @param store
	 * @param callbackAddr
	 * @param ttl
	 * @param ttl_unit
	 * @return
	 * @throws FIAPException
	 */
	public ScheduledFuture<Map<String, Map<Instant, String>>> trapData(Map<String, Map<Instant, String>> store, String callbackAddr, long ttl, TimeUnit ttl_unit) throws FIAPException{
		FIAPServer server = new FIAPServer(
				FIAPServer.NOT_IMPLEMENTED_HANDLER,
				(req) -> {
					Body b = req.getBody();
					if(b==null){
						throw new FIAPException("No body");
					}

					Map<String, Map<Instant, String>> map = createPointMap(b.getPoint());
					for(Entry<String, Map<Instant, String>> e : map.entrySet()){
						if(!store.containsKey(e.getKey())){
							store.put(e.getKey(), e.getValue());
						} else {
							store.get(e.getKey()).putAll(e.getValue());
						}
					}

					return FIAPServer.okTransport(req);
				}
		);

		ScheduledExecutorService service = getScheduledExecutorService();
		Endpoint endpoint = Endpoint.create(server);
		endpoint.publish(callbackAddr);
		ScheduledFuture<Map<String, Map<Instant, String>>>  future = service.schedule(
				() -> {
					endpoint.stop();
					return store;
				},
				ttl,
				ttl_unit
		);

		return future;
	}

	// ------------------------------------------------------------------------

	private static ThreadLocal<DatatypeFactory> dtf = new ThreadLocal<DatatypeFactory>(){
		@Override
		protected DatatypeFactory initialValue() {
			try {
				return DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new InternalError(e);
			}
		};
	};
	private static ScheduledExecutorService __service;

	// ------------------------------------------------------------------------

	private void sumAndNormalizeBody(List<Point> storePoint, List<PointSet> storePointSet, Body add){
		if(storePointSet != null){
			for(PointSet ps : add.getPointSet()){
				Optional<PointSet> mps =
						storePointSet.stream()
						.filter(it -> it.getId().equals(ps.getId()))
						.findFirst();

				if(mps.isPresent()){
					PointSet eps = mps.get();
					eps.getPoint().addAll(ps.getPoint());
					eps.getPointSet().addAll(ps.getPointSet());

				} else {
					storePointSet.add(ps);

				}
			}
		}

		if(storePoint != null){
			for(Point p: add.getPoint()){
				Optional<Point> mp =
						storePoint.stream()
						.filter(it -> it.getId().equals(p.getId()))
						.findFirst();

				if(mp.isPresent()){
					Point ep = mp.get();
					ep.getValue().addAll(p.getValue());

				} else {
					storePoint.add(p);
				}
			}
		}
	}

	private static Map<String, Map<Instant, String>> createPointMap(Iterable<Point> points){
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

	private static List<Point> createPointList(Map<String, Map<Instant, String>> pointValues){
		List<Point> list = new ArrayList<>();
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

			list.add(point);
		}

		return list;
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

	private static synchronized ScheduledExecutorService getScheduledExecutorService(){
		if(__service==null){
			__service = Executors.newSingleThreadScheduledExecutor();
		}
		return __service;
	}

}
