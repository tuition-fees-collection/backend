package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class DailyCollectionCsvService {
    public byte[] generateCsv(DailyCollectionReport report){
        StringBuilder csv=new StringBuilder();
        csv.append("Student ID,Fee Type,Amount,Payment Method,Settled At,Payment Reference\n");
        for(DailyCollectionRecord record:report.items()){
            csv.append(record.studentId()).append(",");
            csv.append(record.feeType()).append(",");
            csv.append(record.amountApplied()).append(",");
            csv.append(record.paymentMethod()).append(",");
            csv.append(record.settledAt()).append(",");
            csv.append(escape(record.paymentReference())).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value){
        if(value==null){
            return"";
        }
        String escaped=value.replace("\"", "\"\"");
        if(escaped.contains(",")
           ||escaped.contains("\"")
            || escaped.contains("\n")
            || escaped.contains("\r")){
                 return "\"" + escaped + "\"";
            }
            return  escaped;
    }
}