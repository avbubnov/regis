package ru.spb.iac.cud.sign;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.WSTrustConstants;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.Base64;
import org.picketlink.identity.federation.core.constants.PicketLinkFederationConstants;
import org.picketlink.identity.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.picketlink.identity.xmlsec.w3.xmldsig.KeyValueType;
import org.picketlink.identity.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.picketlink.identity.xmlsec.w3.xmldsig.SignatureType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

public class GOSTSignatureUtil {

	private static final PicketLinkLogger logger = PicketLinkLoggerFactory
			.getLogger();

	public static void marshall(SignatureType signature, OutputStream os)
			throws JAXBException, SAXException {
		throw logger.notImplementedYet("NYI");
	}

	public static String getXMLSignatureAlgorithmURI(String algo) {
		String xmlSignatureAlgo = null;

		if ("DSA".equalsIgnoreCase(algo)) {
			xmlSignatureAlgo = JBossSAMLConstants.SIGNATURE_SHA1_WITH_DSA.get();
		} else if ("RSA".equalsIgnoreCase(algo)) {
			xmlSignatureAlgo = JBossSAMLConstants.SIGNATURE_SHA1_WITH_RSA.get();
		}
		return xmlSignatureAlgo;
	}

	public static byte[] sign(String stringToBeSigned, PrivateKey signingKey)
			throws GeneralSecurityException {
		if (stringToBeSigned == null)
			throw logger.nullArgumentError("stringToBeSigned");
		if (signingKey == null)
			throw logger.nullArgumentError("signingKey");

		String algo = signingKey.getAlgorithm();

		// Signature sig = getSignature(algo);
		Signature sig = Signature.getInstance("GOST3411withGOST3410EL");

		sig.initSign(signingKey);
		sig.update(stringToBeSigned.getBytes());
		return sig.sign();
	}

	public static boolean validate(byte[] signedContent, byte[] signatureValue,
			PublicKey validatingKey) throws GeneralSecurityException {
		if (signedContent == null)
			throw logger.nullArgumentError("signedContent");
		if (signatureValue == null)
			throw logger.nullArgumentError("signatureValue");
		if (validatingKey == null)
			throw logger.nullArgumentError("validatingKey");

		// We assume that the sigatureValue has the same algorithm as the public
		// key
		// If not, there will be an exception anyway
		String algo = validatingKey.getAlgorithm();
		// Signature sig = getSignature(algo);
		Signature sig = Signature.getInstance("GOST3411withGOST3410EL");

		sig.initVerify(validatingKey);
		sig.update(signedContent);
		return sig.verify(signatureValue);
	}

	public static boolean validate(byte[] signedContent, byte[] signatureValue,
			String signatureAlgorithm, X509Certificate validatingCert)
			throws GeneralSecurityException {
		if (signedContent == null)
			throw logger.nullArgumentError("signedContent");
		if (signatureValue == null)
			throw logger.nullArgumentError("signatureValue");
		if (signatureAlgorithm == null)
			throw logger.nullArgumentError("signatureAlgorithm");
		if (validatingCert == null)
			throw logger.nullArgumentError("validatingCert");

		Signature sig = getSignature(signatureAlgorithm);

		sig.initVerify(validatingCert);
		sig.update(signedContent);
		return sig.verify(signatureValue);
	}

	public static DSAKeyValueType getDSAKeyValue(Element element)
			throws ParsingException {
		DSAKeyValueType dsa = new DSAKeyValueType();
		NodeList nl = element.getChildNodes();
		int length = nl.getLength();

		for (int i = 0; i < length; i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element childElement = (Element) node;
				String tag = childElement.getLocalName();

				byte[] text = childElement.getTextContent().getBytes();

				if (WSTrustConstants.XMLDSig.P.equals(tag)) {
					dsa.setP(text);
				} else if (WSTrustConstants.XMLDSig.Q.equals(tag)) {
					dsa.setQ(text);
				} else if (WSTrustConstants.XMLDSig.G.equals(tag)) {
					dsa.setG(text);
				} else if (WSTrustConstants.XMLDSig.Y.equals(tag)) {
					dsa.setY(text);
				} else if (WSTrustConstants.XMLDSig.SEED.equals(tag)) {
					dsa.setSeed(text);
				} else if (WSTrustConstants.XMLDSig.PGEN_COUNTER.equals(tag)) {
					dsa.setPgenCounter(text);
				}
			}
		}

		return dsa;
	}

	public static RSAKeyValueType getRSAKeyValue(Element element)
			throws ParsingException {
		RSAKeyValueType rsa = new RSAKeyValueType();
		NodeList nl = element.getChildNodes();
		int length = nl.getLength();

		for (int i = 0; i < length; i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element childElement = (Element) node;
				String tag = childElement.getLocalName();

				byte[] text = childElement.getTextContent().getBytes();

				if (WSTrustConstants.XMLDSig.MODULUS.equals(tag)) {
					rsa.setModulus(text);
				} else if (WSTrustConstants.XMLDSig.EXPONENT.equals(tag)) {
					rsa.setExponent(text);
				}
			}
		}

		return rsa;
	}

	public static KeyValueType createKeyValue(PublicKey key) {
		if (key instanceof RSAPublicKey) {
			RSAPublicKey pubKey = (RSAPublicKey) key;
			byte[] modulus = pubKey.getModulus().toByteArray();
			byte[] exponent = pubKey.getPublicExponent().toByteArray();

			RSAKeyValueType rsaKeyValue = new RSAKeyValueType();
			rsaKeyValue.setModulus(Base64.encodeBytes(modulus).getBytes());
			rsaKeyValue.setExponent(Base64.encodeBytes(exponent).getBytes());
			return rsaKeyValue;
		} else if (key instanceof DSAPublicKey) {
			DSAPublicKey pubKey = (DSAPublicKey) key;
			byte[] P = pubKey.getParams().getP().toByteArray();
			byte[] Q = pubKey.getParams().getQ().toByteArray();
			byte[] G = pubKey.getParams().getG().toByteArray();
			byte[] Y = pubKey.getY().toByteArray();

			DSAKeyValueType dsaKeyValue = new DSAKeyValueType();
			dsaKeyValue.setP(Base64.encodeBytes(P).getBytes());
			dsaKeyValue.setQ(Base64.encodeBytes(Q).getBytes());
			dsaKeyValue.setG(Base64.encodeBytes(G).getBytes());
			dsaKeyValue.setY(Base64.encodeBytes(Y).getBytes());
			return dsaKeyValue;
		}
		throw logger.unsupportedType(key.toString());
	}

	private static Signature getSignature(String algo)
			throws GeneralSecurityException {
		Signature sig = null;

		if ("DSA".equalsIgnoreCase(algo)) {
			sig = Signature
					.getInstance(PicketLinkFederationConstants.DSA_SIGNATURE_ALGORITHM);
		} else if ("RSA".equalsIgnoreCase(algo)) {
			sig = Signature
					.getInstance(PicketLinkFederationConstants.RSA_SIGNATURE_ALGORITHM);
		} else
			throw logger.signatureUnknownAlgo(algo);
		return sig;
	}

}
