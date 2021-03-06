package net.iryndin.jdbf.core;

import net.iryndin.jdbf.util.BitUtils;
import net.iryndin.jdbf.util.JdbfUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class DbfRecord {
	private byte[] bytes;
	private DbfMetadata metadata;
	private Charset stringCharset;
	
	public DbfRecord(byte[] source,  DbfMetadata metadata) {
		this.bytes = new byte[source.length];
		System.arraycopy(source, 0, this.bytes, 0, source.length);
		this.metadata = metadata;
	}
	
	public DbfRecord(DbfMetadata metadata) {
		this.metadata = metadata;
		fillBytesFromMetadata();
	}
	
	private void fillBytesFromMetadata() {
		bytes = new byte[metadata.getOneRecordLength()];
		BitUtils.memset(bytes, 0x20);
	}
	
	public Charset getStringCharset() {
		return stringCharset;
	}
	public void setStringCharset(Charset stringCharset) {
		this.stringCharset = stringCharset;
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	public String getString(String fieldName) {
		if (stringCharset == null) {
			stringCharset = Charset.defaultCharset();
		}
		return getString(fieldName, stringCharset);
	}
	public String getString(String fieldName, String charsetName) {
		return getString(fieldName, Charset.forName(charsetName));
	}
	public String getString(String fieldName, Charset charset) {
		DbfField f = getField(fieldName);
		int offset = f.getOffset();
		int actualLength = f.getLength();
		
		// check for empty strings
		{
			for (int i = offset + actualLength-1; i>=offset; i--) {
				if (bytes[i] == JdbfUtils.EMPTY) {
					actualLength--;
				} else {
					break;
				}
			}
			if (actualLength == 0) {
				return null;
			}
		}
		return new String(bytes, offset, actualLength, charset);
		
		/*
		byte[] b = new byte[actualLength];
		System.arraycopy(bytes, offset, b, 0, actualLength);
		// check for empty strings
		{
			for (int i = b.length-1; i>=0; i--) {
				if (b[i] == JdbfUtils.EMPTY) {
					actualLength--;
				} else {
					break;
				}
			}
			if (actualLength == 0) {
				return null;
			}
		}
		return new String(b, 0, actualLength, charset);
		*/
	}
	
	public Date getDate(String fieldName) throws ParseException {
		String s = getString(fieldName);
		if (s == null) {
			return null;
		}
		return JdbfUtils.parseDate(s);
	}
	
	public BigDecimal getBigDecimal(String fieldName) {
		DbfField f = getField(fieldName);
		String s = getString(fieldName);

        if (s == null || s.trim().length() == 0) {
			return null;
		} else {
            s = s.trim();
        }

		//MathContext mc = new MathContext(f.getNumberOfDecimalPlaces());
		//return new BigDecimal(s, mc);
		return new BigDecimal(s);
	}
	public Boolean getBoolean(String fieldName) {
		String s = getString(fieldName);
		if (s == null) {
			return null;
		}
		if (s.equalsIgnoreCase("t")) {
			return Boolean.TRUE;
		} else if (s.equalsIgnoreCase("f")) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}
	public void setBoolean(String fieldName, Boolean value) {
		DbfField f = getField(fieldName);
		// TODO: write boolean		
	}
	
	public byte[] getBytes(String fieldName) {
		DbfField f = getField(fieldName);
		byte[] b = new byte[f.getLength()];
		System.arraycopy(bytes, f.getOffset(), b, 0, f.getLength());
		return b;
	}
	
	public void setBytes(String fieldName, byte[] fieldBytes) {
		DbfField f = getField(fieldName);
		// TODO: 
		// assert fieldBytes.length = f.getLength()
		System.arraycopy(fieldBytes, 0, bytes, f.getOffset(), f.getLength());
	}
	
	public DbfField getField(String fieldName) {
		return metadata.getField(fieldName);
	}
	public Collection<DbfField> getFields() {
		return metadata.getFields();
	}
	public String getStringRepresentation() throws Exception {
		StringBuilder sb = new StringBuilder(bytes.length*10);
		for (DbfField f : getFields()) {
			sb.append(f.getName()).append("=");
			switch(f.getType()) {
			case Character:
			{
				String s = getString(f.getName(), "Cp866");
				//System.out.println(f.getName()+"="+s);
				sb.append(s);
				break;
			}
			case Date:
			{
				Date d = getDate(f.getName());
				//System.out.println(f.getName()+"="+d);
				sb.append(d);
				break;
			}
			case Numeric:
			{
				BigDecimal bd = getBigDecimal(f.getName());
				//System.out.println(f.getName()+"="+(bd != null ? bd.toPlainString() : null));
				sb.append(bd);
				break;
			}
			case Logical:
			{
				Boolean b = getBoolean(f.getName());
				//System.out.println(f.getName()+"="+b);
				sb.append(b);
				break;
			}
			}
			sb.append(", ");
		}
		return sb.toString();
	}
	
	public Map<String, Object> toMap() throws ParseException {
		Map<String, Object> map = new LinkedHashMap<String, Object>(getFields().size()*2);
		for (DbfField f : getFields()) {
			String name = f.getName();
			switch(f.getType()) {
			case Character:
				map.put(name, getString(name));
				break;
			case Date:
				map.put(name, getDate(name));
				break;
			case Numeric:
				map.put(name, getBigDecimal(name));
				break;
			case Logical:
				map.put(name, getBoolean(name));
				break;
			}
		}		
		return map;
	}
}
