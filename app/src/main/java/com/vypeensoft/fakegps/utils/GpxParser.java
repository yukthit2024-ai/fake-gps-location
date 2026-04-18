package com.vypeensoft.fakegps.utils;

import android.util.Xml;

import com.vypeensoft.fakegps.model.LocationPoint;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GpxParser {

    public static List<LocationPoint> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readGpx(parser);
        } finally {
            in.close();
        }
    }

    private static List<LocationPoint> readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<LocationPoint> points = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, null, "gpx");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("trk")) {
                points.addAll(readTrk(parser));
            } else {
                skip(parser);
            }
        }
        return points;
    }

    private static List<LocationPoint> readTrk(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<LocationPoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "trk");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("trkseg")) {
                points.addAll(readTrkSeg(parser));
            } else {
                skip(parser);
            }
        }
        return points;
    }

    private static List<LocationPoint> readTrkSeg(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<LocationPoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "trkseg");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("trkpt")) {
                points.add(readTrkPt(parser));
            } else {
                skip(parser);
            }
        }
        return points;
    }

    private static LocationPoint readTrkPt(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "trkpt");
        String latStr = parser.getAttributeValue(null, "lat");
        String lonStr = parser.getAttributeValue(null, "lon");
        double lat = Double.parseDouble(latStr);
        double lon = Double.parseDouble(lonStr);
        String time = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("time")) {
                time = readText(parser);
            } else {
                skip(parser);
            }
        }
        return new LocationPoint(lat, lon, time);
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
