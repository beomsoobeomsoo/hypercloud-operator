package k8s.example.client.handler;

import java.util.Map;

import org.slf4j.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import k8s.example.client.Constants;
import k8s.example.client.DataObject.TokenCR;
import k8s.example.client.ErrorCode;
import k8s.example.client.Main;
import k8s.example.client.Util;
import k8s.example.client.k8s.K8sApiCaller;

public class NameSpaceHandler extends GeneralHandler {
    private Logger logger = Main.logger;
	@Override
    public Response post(
      UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		logger.info("***** GET /nameSpace");
		
		String outMsg = null;
		IStatus status = null;
		String originUri = null;
		String accessToken = null;
		V1NamespaceList nsList = null;
		String outDO = null; 
		
		try {
			// Read AccessToken from Header
			if(session.getHeaders().get("Authorization") != null) {
				accessToken = session.getHeaders().get("Authorization");
			} else {
				status = Status.BAD_REQUEST;
				throw new Exception(ErrorCode.TOKEN_EMPTY);
			}
    		logger.info( "  Token: " + accessToken );
    		
    		// Verify access token	
			JWTVerifier verifier = JWT.require(Algorithm.HMAC256(Constants.ACCESS_TOKEN_SECRET_KEY)).build();
			DecodedJWT jwt = verifier.verify(accessToken);
			
			String issuer = jwt.getIssuer();
			String userId = jwt.getClaims().get(Constants.CLAIM_USER_ID).asString();
			String tokenId = jwt.getClaims().get(Constants.CLAIM_TOKEN_ID).asString();
			logger.info( "  Issuer: " + issuer );
			logger.info( "  User ID: " + userId );
			logger.info( "  Token ID: " + tokenId );
			
			if(verifyAccessToken(accessToken, userId, tokenId, issuer)) {		
				logger.info( "  Token Validated " );
				nsList = K8sApiCaller.getAccessibleNS(userId);
				status = Status.OK;

			} else {
				logger.info( "  Token is not valid" );
				status = Status.UNAUTHORIZED;
				outDO = "Get NameSpace List failed. Token is not valid.";
			}
    		
			// Make outDO					
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			outDO = gson.toJson( nsList ).toString();
	

		} catch (ApiException e) {
			logger.info( "Exception message: " + e.getMessage() );
			outDO = "Get NameSpace List failed.";
			status = Status.BAD_REQUEST;

		} catch (Exception e) {
			logger.info( "Exception message: " + e.getMessage() );
			e.printStackTrace();
			outDO = "Get NameSpace List failed.";
			status = Status.BAD_REQUEST;
		}
		
//		logger.info();
		return Util.setCors(NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_HTML, outDO));
    }
	
	@Override
    public Response other(
      String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		logger.info("***** OPTIONS /authClient");
		
		return Util.setCors(NanoHTTPD.newFixedLengthResponse(""));
    }
	
	private boolean verifyAccessToken (String accessToken, String userId, String tokenId, String issuer) throws Exception {
		boolean result = false;		
	
		String tokenName = userId.replace("@", "-") + "-" + tokenId;
		TokenCR token = K8sApiCaller.getToken(tokenName);
		
		accessToken = Util.Crypto.encryptSHA256(accessToken);
		
		if(issuer.equals(Constants.ISSUER) &&
				accessToken.equals(token.getAccessToken()))
			result = true;
		
		return result;
	}

}