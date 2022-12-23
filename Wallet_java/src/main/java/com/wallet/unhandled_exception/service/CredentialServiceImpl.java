package com.wallet.unhandled_exception.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wallet.unhandled_exception.constant.CredentialConstant;
import com.wallet.unhandled_exception.constant.CredentialFieldDisclosureValue;
import com.wallet.unhandled_exception.constant.ErrorCode;
import com.wallet.unhandled_exception.constant.ParamKeyConstant;
import com.wallet.unhandled_exception.protocol.base.Credential;
import com.wallet.unhandled_exception.protocol.base.CredentialWrapper;
import com.wallet.unhandled_exception.protocol.request.CreateCredentialArgs;
import com.wallet.unhandled_exception.protocol.response.ResponseData;
import com.wallet.unhandled_exception.util.CredentialUtils;
import com.wallet.unhandled_exception.util.DataToolUtils;
import com.wallet.unhandled_exception.util.DateUtils;




public class CredentialServiceImpl implements CredentialService{
	
    private static final Logger logger = LoggerFactory.getLogger(CredentialServiceImpl.class);

    @Override
	public ResponseData<CredentialWrapper> createCredential(CreateCredentialArgs args){
		
		CredentialWrapper credentialWrapper = new CredentialWrapper();
		try {
			ErrorCode innerResponse = checkCreateCredentialArgsValidity(args, true);
			if (ErrorCode.SUCCESS.getCode() != innerResponse.getCode()) {
				logger.error("Generate Credential input format errror!");
				return new ResponseData<>(null, innerResponse);
			}
			
			Credential result = new Credential();
			String context = CredentialUtils.getDefaultCredentialContext();
			result.setContext(context);
			result.setId(UUID.randomUUID().toString());
			result.setCptId(args.getCptId());
			result.setIssuer(args.getIssuer());
			Long issuanceDate = args.getIssuanceDate();
			if (issuanceDate == null) {
				result.setIssuanceDate(DateUtils.getNoMillisecondTimeStamp());
			} else {

				result.setIssuanceDate(issuanceDate);
			}
			
			Long expirationDate = args.getExpirationDate();
			
			if(expirationDate == null) {
              logger.error("Create Credential Args illegal.");
              return new ResponseData<>(null, ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL);
			} else {
				result.setExpirationDate(expirationDate);
			}
                
            result.setClaim(args.getClaim());
            Map<String, Object> disclosureMap = new HashMap<>(args.getClaim());
            for (Map.Entry<String, Object> entry : disclosureMap.entrySet()) {
            	disclosureMap.put(
            			entry.getKey(),
            			CredentialFieldDisclosureValue.DISCLOSED.getStatus());
            }
            credentialWrapper.setDisclosure(disclosureMap);
            
            Map<String, String> credentialProof = CredentialUtils.buildCredentialProof(
            		result, 
            		args.getIdPrivateKey().getPrivateKey(), 
            		disclosureMap);
            result.setProof(credentialProof);
                        
            credentialWrapper.setCredential(result);
            ResponseData<CredentialWrapper> responseData = new ResponseData<>(
            		credentialWrapper,
            		ErrorCode.SUCCESS);
            
            return responseData;			
			
		} catch(Exception e) {
			logger.error("Generate Credential failed due to system error. ", e);
			return new ResponseData<>(null, ErrorCode.CREDENTIAL_ERROR);
		}
	}
    
    private ErrorCode checkCreateCredentialArgsValidity(
			CreateCredentialArgs args, boolean privateKeyReired) {
		ErrorCode innerResponseData = CredentialUtils.isCreateCredentialArgsValid(args);
		if(ErrorCode.SUCCESS.getCode() != innerResponseData.getCode()) {
			logger.error("Create Credential Args illegal: {}", innerResponseData.getCodeDesc());
			return innerResponseData;
		}
		if (privateKeyReired && StringUtils.isEmpty(args.getIdPrivateKey().getPrivateKey())) {
			logger.error(ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS.getCodeDesc());
			return ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS;
		}
		return ErrorCode.SUCCESS;

	}
    
    
    @Override
    public ResponseData<String> getCredentialJson(Credential credential){
    	
    	ErrorCode errorCode = CredentialUtils.isCredentialValid(credential);
    	if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
    		return new ResponseData<>(
    				StringUtils.EMPTY,
    				ErrorCode.geetTypeByErrorCode(errorCode.getCode()));
    	}
    	try {
    		Map<String, Object> credMap = DataToolUtils.objToMap(credential);
    		String issuanceDate = DateUtils.convertTimestampToDate(credential.getIssuanceDate());
    		String expirationDate = DateUtils.convertTimestampToDate(credential.getExpirationDate());
    		credMap.put(ParamKeyConstant.ISSUANCE_DATE, issuanceDate);
    		credMap.put(ParamKeyConstant.EXPIRATION_DATE, expirationDate);
    		credMap.remove(ParamKeyConstant.CONTEXT);
    		credMap.put(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD,
    				CredentialConstant.DEFAULT_CREDENDTIAL_CONTEXT);
    		String credentialString = DataToolUtils.mapToCompactJson(credMap);
    		
    		return new ResponseData<String>(credentialString, ErrorCode.SUCCESS);
    	} catch (Exception e) {
    		logger.error("Json conversion failed in getCredentialJson: ", e);
    		return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CREDENTIAL_ERROR);
    	}
    }
    
    @Override
    public ResponseData<CredentialWrapper> createSelectiveCredential(
    		Credential credential,
    		String disclosure) {
    	
    	CredentialWrapper credentialResult = new CredentialWrapper();
    	ErrorCode checkResp = CredentialUtils.isCredentialValid(credential);
    	if (ErrorCode.SUCCESS.getCode() != checkResp.getCode()) {
    		return new ResponseData<>(credentialResult, checkResp);
    	}
    	
    	Map<String, Object> claim = credential.getClaim();
    	Map<String, Object> hashMap = new HashMap<String, Object>(claim);
    	
    	for (Map.Entry<String, Object> entry : claim.entrySet()) {
    		claim.put(entry.getKey(), CredentialUtils.getFieldHash(entry.getValue()));
    	}
    	
    	Map<String, Object> disclosureMap = DataToolUtils.deserialize(disclosure, HashMap.class);
    	
    	for (Map.Entry<String, Object> entry : disclosureMap.entrySet()) {
    		if(CredentialFieldDisclosureValue.DISCLOSED.
    				getStatus().equals(entry.getValue())) {
    			claim.put(entry.getKey(), hashMap.get(entry.getKey()));
    		}
    	}    	
    	credentialResult.setCredential(credential);
    	credentialResult.setDisclosure(disclosureMap);
    	
    	return new ResponseData<>(credentialResult, ErrorCode.SUCCESS);    
    }
    
    @Override
    public ResponseData<Boolean> verify(CredentialWrapper credentialWrapper) throws Exception {
    	return verifyCredentialContent(credentialWrapper);
    }
    
    private ResponseData<Boolean> verifyCredentialContent(CredentialWrapper 
    		credentialWrapper) throws Exception {
    	Credential credential = credentialWrapper.getCredential();
    	ErrorCode innerResponse = CredentialUtils.isCredentialValid(credential);
    	if(ErrorCode.SUCCESS.getCode() != innerResponse.getCode()) {
    		logger.error("Credential input format error!");
    		return new ResponseData<>(false, innerResponse);
    	}
    	    	
    	ResponseData<Boolean> responseData = verifyNotExpired(credential);
    	if (!responseData.getResult()) {
    		return responseData;
    	}
    	
    	responseData = verifySignature(credentialWrapper);
    	return responseData;  	
    }
    
    private ResponseData<Boolean> verifyNotExpired(Credential credential){
    	try {
    		boolean result = DateUtils.isAfterCurrentTime(credential.getExpirationDate());
    		ResponseData<Boolean> responseData = new ResponseData<Boolean>(result, ErrorCode.SUCCESS);
    		if(!result) {
    			responseData.setErrorCode(ErrorCode.CREDENTIAL_EXPIRED);
    		}
    		return responseData;
    	} catch (Exception e) {
    		logger.error(
    				"Generic error occured during verify expiration when verifyCredential" + e);
    		
    		return new ResponseData<Boolean>(false, ErrorCode.CREDENTIAL_ERROR);
    	}
    }
    
    private ResponseData<Boolean> verifySignature(
    		CredentialWrapper credentialWrapper) throws Exception{
    	
    		Credential credential = credentialWrapper.getCredential();
    		Map<String, Object> disclosureMap = credentialWrapper.getDisclosure();
    		String rawData = CredentialUtils
    				.getCredentialThumbprintWithoutSig(credential, disclosureMap);
    		    		
    		
    		String publickey = credential.getIssuer();
    		
    		String signature = credential.getSignature();
    		
    		boolean result = DataToolUtils.verifySecp256k1Signature(rawData, 
    				signature, publickey);
    		
    		if (!result) {
    			return new ResponseData<>(false, ErrorCode.CREDENTIAL_VERIFY_FAIL);
    		}
    		
    		return new ResponseData<>(true, ErrorCode.SUCCESS);
    		   	
    }

}
