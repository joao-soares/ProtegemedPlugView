/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package protegemed.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 *
 * @author Joao Antonio Soares
 */
public class ProtegemedCompute implements ProtegemedConstants {

    private final List<String> plugs;
    private final String startDate;
    private final String endDate;
    private final Long maxTime;
    Map<Integer, Long> concurrentUsageTime = new HashMap<>();
    Map<Integer, Integer> concurrences = new HashMap<>();
    Map<Integer, Integer> concurrentUses = new HashMap<>();

    public ProtegemedCompute(List<String> plugs, String startDate, String endDate) {
        this.plugs = plugs;
        this.startDate = startDate;
        this.endDate = endDate;
        
        ProtegemedPropertyLoad property = new ProtegemedPropertyLoad("resources/config.properties");
        this.maxTime = property.getMaxTime();
        
    }

    public Map<Integer, Map<String, String>> executeSearch() throws SQLException {

        Map<Integer, Map<String, String>> result = new HashMap<>();

        Integer captureCode;
        Integer plugCode;
        Integer eventCode;
        Timestamp actualDate;

        Connection conn = ProtegemedConnection.getConnection();
        PreparedStatement ps;

        if (conn != null) {

            try {

                Map<Integer, Boolean> simultaneousEvents = new HashMap<>();
                String plugsAsString = getPlugs().toString();
                plugsAsString = plugsAsString.replace("[", "");
                plugsAsString = plugsAsString.replace("]", "");
                plugsAsString = plugsAsString.replace(", ", ",");

                ps = conn.prepareStatement("SELECT codCaptura, codTomada, codEvento, dataAtual FROM capturaatual WHERE codTomada IN (" + plugsAsString + ") AND codEvento IN ("+ PLUG_ON +", "+ PLUG_OFF +") AND ( dataAtual >= STR_TO_DATE(?,'%d/%m/%Y') AND dataAtual <= STR_TO_DATE(?,'%d/%m/%Y %H:%i:%s') ) ORDER BY dataAtual, codCaptura;");

                ps.setString(1, getStartDate());
                ps.setString(2, getEndDate());

                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst()) {
                    simultaneousEvents = getSimultaneousEvents(conn, getPlugs(), getStartDate(), getEndDate());
                }

                // Map to store last events for plugs
                Map<Integer, Map<Integer, Timestamp>> plugEvents = new HashMap<>();
                
                //Map to store last captureCode for plugs
                Map<Integer, Integer> plugCapture = new HashMap<>();
                
                //Map to store plug on errors
                Map<Integer, Integer> plugOnError = new HashMap<>();
                
                //Map to store plug off errors
                Map<Integer, Integer> plugOffError = new HashMap<>();
                
                //Map to store plug on errors simultaneous
                Map<Integer, Integer> plugOnErrorSimultaneous = new HashMap<>();
                
                //Map to store plug off errors simultaneous
                Map<Integer, Integer> plugOffErrorSimultaneous = new HashMap<>();
                
                //Map to store used time for plugs
                Map<Integer, Long> plugUsedTime = new HashMap<>();
                
                //Map to store use number for plugs
                Map<Integer, Integer> plugUses = new HashMap<>();
                
                //Map to store use number simultaneous for plugs
                Map<Integer, Integer> plugUsesSimultaneous = new HashMap<>();
                
                //Map to store discarded events that exceeded maxTime
                Map<Integer, Integer> plugExceededMaxTime = new HashMap<>();
                
                //Map to store average use time for plugs
                Map<Integer, Long> plugAvgUsedTime = new HashMap<>();
                
                Map<Integer, Long> minUsageTime = new HashMap<>();
                Map<Integer, Long> maxUsageTime = new HashMap<>();

                while (rs.next()) {

                    //Fill loop internal vars with current plug values
                    captureCode = rs.getInt("codCaptura");
                    plugCode = rs.getInt("codTomada");
                    eventCode = rs.getInt("codEvento");
                    actualDate = rs.getTimestamp("dataAtual");

                    Map<Integer, Timestamp> plugEventDate = new HashMap<>();

                    if (!plugEvents.containsKey(plugCode) && eventCode.equals(PLUG_OFF)) {

                        continue;

                    } else if (!plugEvents.containsKey(plugCode)) {

                        plugEventDate.put(eventCode, actualDate);
                        plugEvents.put(plugCode, plugEventDate);
                        plugCapture.put(plugCode, captureCode);
                        continue;

                    }

                    Integer simultaneousError = 0;

                    if (simultaneousEvents.containsKey(plugCapture.get(plugCode))) {

                        simultaneousError = 1;

                    }

                    if (plugEvents.get(plugCode).containsKey(eventCode)) {

                        if (Objects.equals(eventCode, PLUG_ON)) {

                            Integer onError = plugOnError.containsKey(plugCode) ? plugOnError.get(plugCode) : 0;
                            Integer onErrorSimultaneous = plugOnErrorSimultaneous.containsKey(plugCode) ? plugOnErrorSimultaneous.get(plugCode) : 0;
                            onError += 1;
                            onErrorSimultaneous += simultaneousError;
                            plugOnError.put(plugCode, onError);
                            plugOnErrorSimultaneous.put(plugCode, onErrorSimultaneous);

                        } else {

                            Integer offError = plugOffError.containsKey(plugCode) ? plugOffError.get(plugCode) : 0;
                            Integer offErrorSimultaneous = plugOffErrorSimultaneous.containsKey(plugCode) ? plugOffErrorSimultaneous.get(plugCode) : 0;
                            offError += 1;
                            offErrorSimultaneous += simultaneousError;
                            plugOffError.put(plugCode, offError);
                            plugOffErrorSimultaneous.put(plugCode, offErrorSimultaneous);

                        }

                    } else if (plugEvents.get(plugCode).containsKey(PLUG_ON)) {
                        
                        long thisUsedTime = (actualDate.getTime() - plugEvents.get(plugCode).get(PLUG_ON).getTime());
                        
                        if(thisUsedTime < getMaxTime()){
                            
                            if(thisUsedTime == 0){
                                thisUsedTime = 1000;
                            }
                            
                            Integer uses = plugUses.containsKey(plugCode) ? plugUses.get(plugCode) : 0;
                            uses += 1;
                            plugUses.put(plugCode, uses);
                            
                            long usedTime = plugUsedTime.containsKey(plugCode) ? plugUsedTime.get(plugCode) : 0;
                            usedTime += thisUsedTime;
                            plugUsedTime.put(plugCode, usedTime);
                            
                            long avgUsedTime;
                            avgUsedTime = usedTime / uses;
                            plugAvgUsedTime.put(plugCode, avgUsedTime);
                            
                            long minUsage = minUsageTime.containsKey(plugCode) ? minUsageTime.get(plugCode) : -1;
                            long maxUsage = maxUsageTime.containsKey(plugCode) ? maxUsageTime.get(plugCode) : 0;
                            
                            if(minUsage == -1){
                                minUsage = thisUsedTime;
                            }
                            
                            if(thisUsedTime <= minUsage){
                                minUsageTime.put(plugCode, thisUsedTime);
                            }
                            
                            if(thisUsedTime > maxUsage){
                                maxUsageTime.put(plugCode, thisUsedTime);
                            }
                            
                            if (simultaneousEvents.containsKey(plugCapture.get(plugCode))) {

                                Integer usesSimultaneous = plugUsesSimultaneous.containsKey(plugCode) ? plugUsesSimultaneous.get(plugCode) : 0;
                                usesSimultaneous += 1;
                                plugUsesSimultaneous.put(plugCode, usesSimultaneous);

                            }
                            
                        } else {
                            
                            Integer exceededMaxTime = plugExceededMaxTime.containsKey(plugCode) ? plugExceededMaxTime.get(plugCode) : 0;
                            exceededMaxTime += 1;
                            plugExceededMaxTime.put(plugCode, exceededMaxTime);
                            
                        }

                    }

                    plugEventDate.put(eventCode, actualDate);
                    plugEvents.put(plugCode, plugEventDate);
                    plugCapture.put(plugCode, captureCode);

                    Map<String, String> tableValues = new HashMap<>();
                    tableValues.put("uses", plugUses.containsKey(plugCode) ? plugUses.get(plugCode).toString() : "0");
                    tableValues.put("usesSimultaneous", plugUsesSimultaneous.containsKey(plugCode) ? plugUsesSimultaneous.get(plugCode).toString() : "0");
                    tableValues.put("concurrentUses", getConcurrentUses().containsKey(plugCode) ? getConcurrentUses().get(plugCode).toString() : "0");
                    tableValues.put("concurrences", getConcurrences().containsKey(plugCode) ? getConcurrences().get(plugCode).toString() : "0");
                    tableValues.put("concurrentUsageTime", getConcurrentUsageTime().containsKey(plugCode) ? msToString(getConcurrentUsageTime().get(plugCode)) : "00:00");
                    tableValues.put("usedTime", plugUsedTime.containsKey(plugCode) ? msToString(plugUsedTime.get(plugCode)) : "00:00");
                    tableValues.put("avgUsedTime", plugAvgUsedTime.containsKey(plugCode) ? msToString(plugAvgUsedTime.get(plugCode)) : "00:00");
                    tableValues.put("minUsageTime", minUsageTime.containsKey(plugCode) ? msToString(minUsageTime.get(plugCode)) : "00:00");
                    tableValues.put("maxUsageTime", maxUsageTime.containsKey(plugCode) ? msToString(maxUsageTime.get(plugCode)) : "00:00");
                    tableValues.put("exceededTimeDiscarded", plugExceededMaxTime.containsKey(plugCode) ? plugExceededMaxTime.get(plugCode).toString() : "0");
                    tableValues.put("onError", plugOnError.containsKey(plugCode) ? plugOnError.get(plugCode).toString() : "0");
                    tableValues.put("onErrorSimultaneous", plugOnErrorSimultaneous.containsKey(plugCode) ? plugOnErrorSimultaneous.get(plugCode).toString() : "0");
                    tableValues.put("offError", plugOffError.containsKey(plugCode) ? plugOffError.get(plugCode).toString() : "0");
                    tableValues.put("offErrorSimultaneous", plugOffErrorSimultaneous.containsKey(plugCode) ? plugOffErrorSimultaneous.get(plugCode).toString() : "0");
                    result.put(plugCode, tableValues);

                }

            } catch (SQLException e) {

                throw e;

            } finally {

                ProtegemedConnection.closeConnection(conn);

            }

        } else {

            throw new SQLException();

        }

        return result;

    }

    private Map<Integer, Boolean> getSimultaneousEvents(Connection conn, List<String> plugs,
            String startDate, String endDate) throws SQLException {

        Map<Integer, Boolean> simultaneousEvents = new HashMap<>();

        Map<Integer, Integer> nextCapture = new HashMap<>();
        Map<Integer, Integer> lastPlugCapture = new HashMap<>();
        Map<Timestamp, List<Integer>> captureDates = new TreeMap<>();
        Map<Integer, TreeMap<Integer, Integer>> captureEvents = new TreeMap<>();
        Map<Integer, Timestamp> simpleCaptureTime = new HashMap<>();
        Map<Integer, Long> localConcurrentUsageTime = new HashMap<>();

        Integer captureCode, plugCode, eventCode;
        Timestamp actualDate;

        String plugsAsString = plugs.toString();
        plugsAsString = plugsAsString.replace("[", "");
        plugsAsString = plugsAsString.replace("]", "");
        plugsAsString = plugsAsString.replace(", ", ",");

        PreparedStatement ps = conn.prepareStatement("SELECT codCaptura, codTomada, codEvento, dataAtual FROM capturaatual WHERE codTomada IN (" + plugsAsString + ") AND codEvento IN ("+ PLUG_ON +", "+ PLUG_OFF +") AND ( dataAtual >= STR_TO_DATE(?,'%d/%m/%Y') AND dataAtual <= STR_TO_DATE(?,'%d/%m/%Y %H:%i:%s') ) ORDER BY dataAtual, codCaptura;");

        ps.setString(1, startDate);
        ps.setString(2, endDate);

        ResultSet rs = ps.executeQuery();
        
        Map<Integer, Integer> sameSimultaneous = new HashMap<>();

        while (rs.next()) {

            captureCode = rs.getInt("codCaptura");
            plugCode = rs.getInt("codTomada");
            eventCode = rs.getInt("codEvento");
            actualDate = rs.getTimestamp("dataAtual");

            if (lastPlugCapture.containsKey(plugCode)) {
                nextCapture.put(lastPlugCapture.get(plugCode), captureCode);
            }

            lastPlugCapture.put(plugCode, captureCode);

            List<Integer> captures = captureDates.containsKey(actualDate) ? captureDates.get(actualDate) : new ArrayList<>();
            captures.add(captureCode);
            captureDates.put(actualDate, captures);

            TreeMap<Integer, Integer> event = new TreeMap<>();
            event.put(plugCode, eventCode);
            captureEvents.put(captureCode, event);
            simpleCaptureTime.put(captureCode, actualDate);

        }
        
        Boolean hasSimultaneous;
        lastPlugCapture.clear();
        Map<Integer, Integer> concurrentPlugs = new HashMap<>();

        for (Entry<Timestamp, List<Integer>> entry : captureDates.entrySet()) {
            
            hasSimultaneous = false;

            List<Integer> capturesOnDate = entry.getValue();
            List<Integer> auxCapturesOnDate = entry.getValue();

            if (!(capturesOnDate.size() > 1)) {
                hasSimultaneous = false;
            }

            for (Integer capture : capturesOnDate) {

                Integer thisCaptureCode, nextCaptureCode;
                Integer thisPlugCode, nextPlugCode;
                Integer thisEventCode, nextEventCode;
                Boolean correctEntry = false;

                thisCaptureCode = capture;
                thisPlugCode = captureEvents.get(thisCaptureCode).firstKey();
                thisEventCode = captureEvents.get(thisCaptureCode).get(thisPlugCode);
                
                for(Integer auxCapture : auxCapturesOnDate){
                    if(!Objects.equals(auxCapture, capture)){
                        Integer auxPlugCode = captureEvents.get(auxCapture).firstKey();
                        Integer auxEventCode = captureEvents.get(auxCapture).get(auxPlugCode);
                        
                        if(Objects.equals(thisEventCode, auxEventCode)){
                            hasSimultaneous = true;
                        }

                    }
                }
                
                nextCaptureCode = nextCapture.get(capture);
                if (nextCaptureCode != null) {
                    nextPlugCode = captureEvents.get(nextCaptureCode).firstKey();
                    nextEventCode = captureEvents.get(nextCaptureCode).get(nextPlugCode);

                    if (Objects.equals(thisPlugCode, nextPlugCode) && !Objects.equals(thisEventCode, nextEventCode)) {
                        correctEntry = true;

                        if(Objects.equals(thisEventCode, PLUG_ON)){
                            
                            Map<Integer, Integer> concurrentPlug = new HashMap<>();
                            
                            for(Entry<Integer, Integer> plugEntry : lastPlugCapture.entrySet()){
                                Integer entryPlugCode = plugEntry.getKey();
                                Integer entryCaptureCode = plugEntry.getValue();
                                Integer entryEventCode = captureEvents.get(entryCaptureCode).get(entryPlugCode);
                                
                                if(Objects.equals(entryPlugCode, thisPlugCode) || Objects.equals(entryEventCode, PLUG_OFF)){
                                    continue;
                                }
                                
                                Integer nextEntryCaptureCode = nextCapture.get(entryCaptureCode);
                                Integer nextEntryPlugCode = captureEvents.get(nextEntryCaptureCode).firstKey();
                                Integer nextEntryEventCode = captureEvents.get(nextEntryCaptureCode).get(nextEntryPlugCode);
                                
                                if(Objects.equals(entryPlugCode, nextEntryPlugCode) && !Objects.equals(entryEventCode, nextEntryEventCode)){
                                    Timestamp thisStartDate = entry.getKey();
                                    Timestamp thisEndDate = simpleCaptureTime.get(nextCaptureCode);
                                    Timestamp nextEndDate = simpleCaptureTime.get(nextEntryCaptureCode);
                                    Timestamp effectiveEndDate = thisEndDate.after(nextEndDate) ? nextEndDate : thisEndDate;
                                    
                                    long interval = effectiveEndDate.getTime() - thisStartDate.getTime();
                                    long lastInterval = localConcurrentUsageTime.containsKey(thisCaptureCode) ? localConcurrentUsageTime.get(thisCaptureCode) : interval;
                                    
                                    if(interval <= lastInterval){
                                        concurrentPlug.put(entryPlugCode, entryCaptureCode);
                                    } 

                                    localConcurrentUsageTime.put(thisCaptureCode, interval < lastInterval ? interval : lastInterval);
                                }
                            }

                            long plugInterval = getConcurrentUsageTime().containsKey(thisPlugCode) ? getConcurrentUsageTime().get(thisPlugCode) : 0;
                            long newInterval = localConcurrentUsageTime.containsKey(thisCaptureCode) ? localConcurrentUsageTime.get(thisCaptureCode) : 0;
                            plugInterval += newInterval;
                            getConcurrentUsageTime().put(thisPlugCode, plugInterval);
                            
                            Integer localConcurrentUsages = getConcurrences().containsKey(thisPlugCode) ? getConcurrences().get(thisPlugCode) : 0;
                            if(newInterval > 0){
                                localConcurrentUsages += 1;
                                getConcurrences().put(thisPlugCode, localConcurrentUsages);
                                concurrentPlugs.put(thisCaptureCode, thisPlugCode);
                            }
                            
                            for(Entry<Integer, Integer> concurrentEntry : concurrentPlug.entrySet()){
                                long otherPlugInterval = getConcurrentUsageTime().containsKey(concurrentEntry.getKey()) ? getConcurrentUsageTime().get(concurrentEntry.getKey()) : 0;
                                newInterval = localConcurrentUsageTime.containsKey(thisCaptureCode) ? localConcurrentUsageTime.get(thisCaptureCode) : 0;
                                otherPlugInterval += newInterval;
                                getConcurrentUsageTime().put(concurrentEntry.getKey(), otherPlugInterval);
                                
                                if(newInterval > 0){
                                    localConcurrentUsages = getConcurrences().containsKey(concurrentEntry.getKey()) ? getConcurrences().get(concurrentEntry.getKey()) : 0;
                                    localConcurrentUsages += 1;
                                    getConcurrences().put(concurrentEntry.getKey(), localConcurrentUsages);
                                    concurrentPlugs.put(concurrentEntry.getValue(), concurrentEntry.getKey());
                                }
                            }
                        }
                    }

                } else {

                    correctEntry = true;

                }

                if(hasSimultaneous){
                    simultaneousEvents.put(capture, correctEntry);
                }
                
                lastPlugCapture.put(thisPlugCode, thisCaptureCode);

            }

        }
        
        for(Entry<Integer, Integer> concurrentEntry : concurrentPlugs.entrySet()){
            Integer concurrences = getConcurrentUses().containsKey(concurrentEntry.getValue()) ? getConcurrentUses().get(concurrentEntry.getValue()) : 0;
            concurrences += 1;
            getConcurrentUses().put(concurrentEntry.getValue(), concurrences);
        }

        return simultaneousEvents;
    }

    private static String msToString(long ms) {

        long totalSecs = ms / 1000;
        long hours = (totalSecs / 3600);
        long mins = (totalSecs / 60) % 60;
        long secs = totalSecs % 60;
        String minsString = (mins == 0)
                ? "00"
                : ((mins < 10)
                        ? "0" + mins
                        : "" + mins);
        String secsString = (secs == 0)
                ? "00"
                : ((secs < 10)
                        ? "0" + secs
                        : "" + secs);
        if (hours > 0) {
            return hours + ":" + minsString + ":" + secsString;
        } else if (mins > 0) {
            return mins + ":" + secsString;
        } else {
            return "0:" + secsString;
        }
    }

    public List<String> getPlugs() {
        return plugs;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public Long getMaxTime() {
        return maxTime;
    }
    
    public Map<Integer, Long> getConcurrentUsageTime() {
        return concurrentUsageTime;
    }

    public void setConcurrentUsageTime(Map<Integer, Long> concurrentUsageTime) {
        this.concurrentUsageTime = concurrentUsageTime;
    }

    public Map<Integer, Integer> getConcurrences() {
        return concurrences;
    }

    public void setConcurrences(Map<Integer, Integer> concurrences) {
        this.concurrences = concurrences;
    }

    public Map<Integer, Integer> getConcurrentUses() {
        return concurrentUses;
    }

    public void setConcurrentUses(Map<Integer, Integer> concurrentUses) {
        this.concurrentUses = concurrentUses;
    }

}
