package com.mateusz.niwa.pathcollector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Trace {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private class Point {
        private float x, y;
        private long t;

        Point(float x, float y, long t) {
            this.x = x;
            this.y = y;
            this.t = t;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public long getT() { return t; }

        public void printXml(StringBuilder sb) {
            sb.append("<point x=\"");
            sb.append(x);
            sb.append("\" y=\"");
            sb.append(y);
            sb.append("\" t=\"");
            sb.append(t);
            sb.append("\"/>\n");
        }
    }

    private ArrayList<Point> mPoints;
    private final long mStartMillis;
    private Date mDate;

    Trace() {
        mStartMillis = System.currentTimeMillis();
        mDate = new Date();
        mPoints = new ArrayList<Point>();
    }

    Trace(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element documentElement = document.getDocumentElement();

            mStartMillis = Long.parseLong(documentElement.getAttribute("ms"));

            SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
            mDate = parser.parse(documentElement.getAttribute("date"));

            mPoints = new ArrayList<Point>();

            NodeList pointNodes = document.getElementsByTagName("point");

            for (int i = 0 ; i < pointNodes.getLength() ; ++i) {
                Node pointNode = pointNodes.item(i);

                if (pointNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element pointElement = (Element) pointNode;
                    float x = Float.parseFloat(pointElement.getAttribute("x"));
                    float y = Float.parseFloat(pointElement.getAttribute("y"));
                    long t = Long.parseLong(pointElement.getAttribute("t"));
                    mPoints.add(new Point(x, y, t));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while parsing trace xml");
        }
    }

    public void appendPoint(float x, float y) {
        mPoints.add(new Point(x, y, System.currentTimeMillis() - mStartMillis));
    }

    public void appendPoint(float x, float y, long timeInMillis) {
        mPoints.add(new Point(x, y, timeInMillis - mStartMillis));
    }

    public String toXml() {
        StringBuilder sb = new StringBuilder(1024);
        Format formatter = new SimpleDateFormat(DATE_FORMAT);

        sb.append("<trace ms=\"" + mStartMillis + "\" date=\"" + formatter.format(mDate) + "\">\n");

        for (Point point : mPoints) {
            point.printXml(sb);
        }

        sb.append("</trace>");

        return sb.toString();
    }

    public Date getDate() { return mDate; }
    public Long getID() { return mStartMillis; }
}