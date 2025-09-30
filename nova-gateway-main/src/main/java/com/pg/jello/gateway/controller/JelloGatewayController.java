package com.pg.jello.gateway.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.jello.gateway.JelloGatewayProcessor;
import com.pg.jello.gateway.exception.JelloGateWayException;
import com.pg.jello.gateway.model.PaymentDetails;
import com.pg.jello.gateway.service.PaymentDetailsService;

@RestController
public class JelloGatewayController {

	private static final Logger logger = LoggerFactory.getLogger(JelloGatewayController.class);
	
	@Autowired
	PaymentDetailsService paymentDetailsService;

	@Autowired
	JelloGatewayProcessor jelloGatewayProcessor;
	
	@RestController
	@RequestMapping("/jellogateway")
	public class Controller {

		@RequestMapping(value = "/ping", method = { RequestMethod.GET, RequestMethod.POST })
		public String ping(ModelMap map) {
			return "pong";
		}

	}

	@RequestMapping(value = "/v6/actions", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> processActions(@RequestBody String payload) {
   // 	logger.info("payload : " + payload);
    	try {
			logger.info("received email request :{}", payload);
    		JSONObject response = jelloGatewayProcessor.process(payload);
        	return ResponseEntity.ok(response.toJSONString());
    	} catch (JelloGateWayException e) {
    		return ResponseEntity.status(e.getErrorCode()).body(e.getErrorMessage());
    	}
    }

	// @Autowired
	// private RestTemplate restTemplate;

	//Wrapper API for processActions
	@RequestMapping(value = "/v6/actions/wrapper", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<String> processActionsWrapper(@RequestBody String payload , @RequestParam(value = "access_token", required = true) String accessToken) {
		 logger.info("[Wrapper] Calling /v6/actions with token {}", accessToken);
		// option:1
		ResponseEntity<String> response = processActions(payload);
		return response;

		// option:2
		// HttpHeaders headers = new HttpHeaders();
		// headers.setContentType(MediaType.APPLICATION_JSON);
		// HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		// // URL
		// String url = "http://localhost:8080/v6/actions";  
		// ResponseEntity<String> restResponse = restTemplate.postForEntity(url, entity, String.class);
		//return restResponse;	
	}
	
// @Bean
// public RestTemplate restTemplate() {
//     return new RestTemplate();
// }

	@RequestMapping(value = "/jellogateway/paymentstatus", method = RequestMethod.GET)
	public String getPaymentStatus(@RequestParam("transactionId") String transactionId) {
		logger.info("inside getPaymentStatus ..");
		PaymentDetails paymentDetails = paymentDetailsService.getByTransactionId(transactionId);
		JSONObject paymentStatus = new JSONObject();
		paymentStatus.put("OrderId", paymentDetails.getOrderId());
		paymentStatus.put("Amount", paymentDetails.getAmount());
		paymentStatus.put("FirstName", paymentDetails.getFirstName());
		paymentStatus.put("LastName", paymentDetails.getLastName());
		paymentStatus.put("Status", paymentDetails.getStatus());
		paymentStatus.put("transactionId", transactionId);
		paymentStatus.put("Email", paymentDetails.getEmail());
		paymentStatus.put("timestamp", new Date(System.currentTimeMillis()).toString());
		paymentStatus.put("timeZone", paymentDetails.getTimezone());
		
		JSONObject response = new JSONObject();
		response.put("responseDesc", "Payment Transactions Found");
		response.put("responseCode", "PAYMENT_RESPONSE_005");
		
		JSONObject payment = new JSONObject();
		payment.put("response", response);
		payment.put("payment", paymentStatus);
		

		return payment.toJSONString();
	}
	
	@RequestMapping(value = "/jellogateway/paymentstatus", method = RequestMethod.POST)
	public ResponseEntity<String> createPaymentStatus(@RequestBody String payload) {
		logger.info("[jello-gateway] Received request to create payment status");
		String response = "";
		ObjectMapper mapper = new ObjectMapper();
		try {
			PaymentDetails paymentDetails = mapper.readValue(payload, PaymentDetails.class);
			PaymentDetails createdPaymentDetails = paymentDetailsService.create(paymentDetails);
			response = mapper.writeValueAsString(createdPaymentDetails);
		} catch (JelloGateWayException e) {
			return ResponseEntity.status(e.getErrorCode()).body(e.getErrorMessage());
		} catch (Exception e) {
			logger.error("Unable to create Payment details for the payload - {}", payload, e);
			return new ResponseEntity<>("Unable to create payment details", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.ok(response);
	}


}
