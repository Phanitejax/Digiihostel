package com.digiihostel.app;

import java.util.*;

/** Fixed lists used by the student forms: schools and the courses (branches) offered by each school. */
class Catalog {
    static final String[] SCHOOLS = {
            "School of Engineering & Sciences(SOES)",
            "School of Management(SOM)",
            "School of Law(SOL)",
            "School of Liberal Arts(SOLA)",
            "School of Agricultural Sciences(SOAS)",
            "School of Hospitality & Tourism(SOHT)",
            "School of Healthcare and Allied Sciences(SOHAS)",
            "UID – School of Design",
            "Centre of Excellence in Occupational Health, Safety, Fire & Environment (C-OHSFE)",
            "Centre of Aerospace and Energy Studies (SOES)"
    };

    static final Map<String, String[]> BRANCHES = new LinkedHashMap<>();
    static {
        BRANCHES.put(SCHOOLS[0], new String[]{"B.Tech Computer Science and Engineering", "B.Tech Civil Engineering",
                "B.Tech Electronics and Communication Engineering", "B.Tech Mechanical Engineering (Electric Vehicles specialisation)",
                "B.Tech Biotechnology", "M.Tech Aerospace Engineering", "M.Tech Computational Mechanics",
                "MCA (Blockchain Technologies)", "M.Sc. Forensic Science", "M.Sc. Microbiology"});
        BRANCHES.put(SCHOOLS[1], new String[]{"BBA", "B.Com", "MBA", "MBA Agriculture Business Management",
                "MBA Aviation Management", "MBA Energy Management", "MBA ESG and Sustainability Management", "Ph.D. Management"});
        BRANCHES.put(SCHOOLS[2], new String[]{"BA LLB (Hons.)", "BBA LLB (Hons.)", "LLB (Hons.)", "LLM", "Ph.D. Law"});
        BRANCHES.put(SCHOOLS[3], new String[]{"BA (Hons.) Psychology", "B.Sc. (Hons.) Clinical Psychology", "BA (Hons.) Liberal Arts",
                "MA Psychology", "MA Clinical Psychology", "MA Political Science", "Professional Diploma in Clinical Psychology",
                "Ph.D. Psychology"});
        BRANCHES.put(SCHOOLS[4], new String[]{"B.Sc. (Hons.) Agriculture", "M.Sc. Agriculture", "M.Sc. Agronomy",
                "M.Sc. Genetics and Plant Breeding", "M.Sc. Horticulture (Vegetable Science)", "M.Sc. Agricultural Extension Education",
                "M.Sc. Floriculture and Landscaping", "MBA Agriculture Business Management", "Ph.D. Agriculture"});
        BRANCHES.put(SCHOOLS[5], new String[]{"BBA (Hons.) Hospitality Management", "BA (Hons.) Culinary Arts", "Diploma in Culinary Arts",
                "Diploma in Pastry Arts", "MBA Hospitality and Tourism", "Ph.D. Hospitality and Tourism"});
        BRANCHES.put(SCHOOLS[6], new String[]{"B.Pharm", "BPT (Bachelor of Physiotherapy)", "B.Sc. Nursing", "B.Optom (Bachelor of Optometry)",
                "BMRIT", "BMLS", "B.Sc. Cardiovascular Technology", "Integrated M.Sc. Clinical Embryology", "M.Pharm Pharmaceutics",
                "M.Pharm Pharmacology", "M.Pharm Pharmacognosy", "MPT (Musculoskeletal Sciences / Neurosciences)", "M.Optom",
                "M.Sc. Clinical Embryology", "Master of Public Health (MPH)", "Postgraduate Diploma in Clinical Embryology",
                "Ph.D. programmes"});
        BRANCHES.put(SCHOOLS[7], new String[]{"B.Des. (Hons.) Fashion Design and Technology", "B.Des. (Hons.) Communication Design",
                "B.Des. (Hons.) Interior Architecture and Design", "B.Des. (Hons.) Animation and Film Making",
                "B.Des. (Hons.) Product and Service Design", "M.Des. Interior Architecture and Design", "M.Des. Communication Design",
                "M.Des. Product and Service Design", "M.Des. Fashion Design and Technology", "Ph.D. Design"});
        BRANCHES.put(SCHOOLS[8], new String[]{"B.Tech Fire and Safety Engineering", "B.Tech Environmental Engineering",
                "M.Tech Health, Safety and Environmental Engineering", "M.Tech Environmental Engineering",
                "MBA ESG and Sustainability Management"});
        BRANCHES.put(SCHOOLS[9], new String[]{"B.Tech Aerospace Engineering", "B.Tech Aerospace Engineering with Pathway to Pilot Programme",
                "B.Sc. Aviation (in association with Redbird Aviation – CPL)", "B.Tech Energy Engineering", "M.Tech Aerospace Engineering"});
    }

    static String[] branchesOf(String school) {
        String[] b = BRANCHES.get(school);
        return b == null ? new String[0] : b;
    }
}
