package com.furkan.scheduler.ingest;

import java.util.List;

public final class DeptCatalog {

    private DeptCatalog() {}

    public record Dept(String code, String name) {}

    // The list is generated using the 'code' and 'label' fields from the JSON.
    public static final List<Dept> ALL = List.of(
            new Dept("AD", "MANAGEMENT"),
            new Dept("ASIA", "ASIAN STUDIES"),
            new Dept("ATA", "ATATURK INSTITUTE FOR MODERN TURKISH HISTORY"),
            new Dept("BIO", "MOLECULAR BIOLOGY & GENETICS"),
            new Dept("BIS", "BUSINESS INFORMATION SYSTEMS"),
            new Dept("BM", "BIOMEDICAL ENGINEERING"),
            new Dept("CCS", "CRITICAL AND CULTURAL STUDIES"),
            new Dept("CE", "CIVIL ENGINEERING"),
            new Dept("CEM", "CONSTRUCTION ENGINEERING AND MANAGEMENT"),
            new Dept("CET", "COMPUTER EDUCATION & EDUCATIONAL TECHNOLOGY"),
            new Dept("CHE", "CHEMICAL ENGINEERING"),
            new Dept("CHEM", "CHEMISTRY"),
            new Dept("CMPE", "COMPUTER ENGINEERING"),
            new Dept("COGS", "COGNITIVE SCIENCE"),
            new Dept("CSE", "COMPUTATIONAL SCIENCE & ENGINEERING"),
            new Dept("DSAI", "DATA SCIENCE AND ARTIFICIAL INTELLIGENCE"),
            new Dept("EC", "ECONOMICS"),
            new Dept("ED", "EDUCATIONAL SCIENCES"),
            new Dept("EE", "ELECTRICAL & ELECTRONICS ENGINEERING"),
            new Dept("EF", "ECONOMICS AND FINANCE"),
            new Dept("ENV", "ENVIRONMENTAL SCIENCES"),
            new Dept("ENVT", "ENVIRONMENTAL TECHNOLOGY"),
            new Dept("EQE", "EARTHQUAKE ENGINEERING"),
            new Dept("ETM", "ENGINEERING AND TECHNOLOGY MANAGEMENT"),
            new Dept("FE", "FINANCIAL ENGINEERING"),
            new Dept("FILM", "FILM AND MEDIA STUDIES"),
            new Dept("FLED", "FOREIGN LANGUAGE EDUCATION"),
            new Dept("GED", "GEODESY"),
            new Dept("GPH", "GEOPHYSICS"),
            new Dept("GUID", "GUIDANCE & PSYCHOLOGICAL COUNSELING"),
            new Dept("HIST", "HISTORY"),
            new Dept("HUM", "HUMANITIES COURSES COORDINATOR"),
            new Dept("IE", "INDUSTRIAL ENGINEERING"),
            new Dept("INT", "CONFERENCE INTERPRETING"),
            new Dept("INTT", "INTERNATIONAL TRADE"),
            new Dept("LAW", "LAW PR."),
            new Dept("LING", "LINGUISTICS"),
            new Dept("LL", "ENGLISH LITERATURE"),
            new Dept("LL", "WESTERN LANGUAGES & LITERATURES"),
            new Dept("LS", "LEARNING SCIENCES"),
            new Dept("MATH", "MATHEMATICS"),
            new Dept("ME", "MECHANICAL ENGINEERING"),
            new Dept("MECA", "MECHATRONICS ENGINEERING (WITH THESIS)"),
            new Dept("MIR", "INTERNATIONAL RELATIONS:TURKEY,EUROPE AND THE MIDDLE EAST"),
            new Dept("MIS", "MANAGEMENT INFORMATION SYSTEMS"),
            new Dept("PA", "FINE ARTS"),
            new Dept("PE", "PHYSICAL EDUCATION"),
            new Dept("PHIL", "PHILOSOPHY"),
            new Dept("PHYS", "PHYSICS"),
            new Dept("POLS", "POLITICAL SCIENCE&INTERNATIONAL RELATIONS"),
            new Dept("PRED", "EARLY CHILDHOOD EDUCATION"),
            new Dept("PRSO", "UNDERGRADUATE PROGRAM IN PRESCHOOL EDUCATION"),
            new Dept("PSY", "PSYCHOLOGY"),
            new Dept("SCED", "MATHEMATICS AND SCIENCE EDUCATION"),
            new Dept("SCO", "SYSTEMS & CONTROL ENGINEERING"),
            new Dept("SOC", "SOCIOLOGY"),
            new Dept("SWE", "SOFTWARE ENGINEERING"),
            new Dept("TK", "TURKISH COURSES COORDINATOR"),
            new Dept("TKL", "TURKISH LANGUAGE & LITERATURE"),
            new Dept("TR", "TRANSLATION AND INTERPRETING STUDIES"),
            new Dept("TRM", "SUSTAINABLE TOURISM MANAGEMENT"),
            new Dept("TRM", "TOURISM MANAGEMENT"),
            new Dept("WTR", "TRANSLATION"),
            new Dept("XMBA", "EXECUTIVE MBA")
    );
}