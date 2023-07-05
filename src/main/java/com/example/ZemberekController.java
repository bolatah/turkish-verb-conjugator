package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.RootLexicon;

@Controller
public class ZemberekController {

    private String lastVerb; // Store the last submitted verb

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/result")
    public String handleResult(@RequestParam(required = false) String verb, String tense, String verbType,
            Model model) {
        if (verb == null && lastVerb == null) {

            String errorMessage = "The 'verb' parameter is required.";
            model.addAttribute("error", errorMessage);
            return "error"; // Assuming you have an "error" view to display the error message
        } else {
            if (verb != null) {
                lastVerb = verb;
            }

            if (lastVerb.length() < 4) {
                String errorMessage = "The verb must be at least 4 characters including '-mek' or '-mak' ";
                model.addAttribute("error", errorMessage);
            }

            if (lastVerb == null) {
                String errorMessage = "The 'verb' parameter is required.";
                model.addAttribute("error", errorMessage);
                return "error";
            }
            if (!verb.endsWith("mek") && !verb.endsWith("mak") && !lastVerb.endsWith("mek")
                    && !lastVerb.endsWith("mak")) {
                String errorMessage = "The 'verb' must be in infinitive form that finishes with '-mek' or '-mak'.";
                model.addAttribute("error", errorMessage);
                return "error";
            }

            String[] positiveNegatives = { "", "Neg" };
            String[] times = { "", "Imp", "Aor", "Past", "Prog1", "Prog2", "Narr", "Fut" };
            String[] persons = { "A1sg", "A2sg", "A3sg", "A1pl", "A2pl", "A3pl" };
            String[] turkishPersons = { "Ben", "Sen", "O", "Biz", "Siz", "Onlar" };
            String[] turkishPersonsForImp = { "Sen", "Sen", "O", "Siz", "Siz", "Siz", "Onlar" };
            String[] turkishPersonsForImpPos = { "Sen", "Sen", "O", "Siz", "Siz", "Onlar" };
            TurkishMorphology morphologyWithDefaultLexicon = TurkishMorphology.builder()
                    .setLexicon(RootLexicon.getDefault())
                    .disableCache().build();

            TurkishMorphology morphology = TurkishMorphology.builder().setLexicon(lastVerb)
                    .disableCache().build();

            List<Result> results = new ArrayList<>();

            for (String posNeg : positiveNegatives) {
                for (String time : times) {
                    for (String person : persons) {

                        // Add conditions to filter the verb type and selected tense
                        if (verbType.equals("positive") && posNeg.equals("Neg")) {
                            continue; // Skip negative conjugations if the verb type is positive
                        }
                        if (verbType.equals("negative") && posNeg.equals("")) {
                            continue; // Skip negative conjugations if the verb type is positive
                        }
                        if (!tense.isEmpty() && !time.equals(tense)) {
                            continue; // Skip conjugations that don't match the selected tense
                        }

                        List<String> seq = Stream.of(posNeg, time, person)
                                .filter(s -> s.length() > 0)
                                .collect(Collectors.toList());

                        String stem = lastVerb.substring(0, lastVerb.length() - 3);
                        String modifiedStem = lastVerb.substring(0, lastVerb.length() - 4) + "d";
                        List<Result> verbResults;

                        if (stem.endsWith("t") && (seq.get(0) == "Prog1" || seq.get(0) == "Aor"
                                || seq.get(0) == "Fut")) {
                            verbResults = morphologyWithDefaultLexicon.getWordGenerator().generate(modifiedStem, seq);
                        } else if (stem.endsWith("r") && seq.get(0) == "Aor") {
                            TurkishMorphology morphologyWithCreateDefaults = TurkishMorphology.createWithDefaults();
                            verbResults = morphologyWithCreateDefaults.getWordGenerator().generate(stem, seq);
                        } else if (seq.get(0) == "Imp" && stem.endsWith("t") && seq.get(1) == "A2pl") {
                            verbResults = morphologyWithDefaultLexicon.getWordGenerator().generate(modifiedStem,
                                    seq);
                        } else {
                            verbResults = morphology.getWordGenerator().generate(stem, seq);
                            System.out.println(seq);
                        }
                        if (verbResults.size() == 0) {
                            System.out.println("Cannot generate Stem = [" + stem + "] Morphemes = " + seq);
                            continue;
                        }

                        if (seq.get(0) == "Imp") {
                            model.addAttribute("persons", turkishPersonsForImpPos);
                        } else if ((seq.get(0) == "Neg" && seq.get(1) == "Imp")) {
                            model.addAttribute("persons", turkishPersonsForImp);
                        } else {
                            model.addAttribute("persons", turkishPersons);
                        }
                        results.addAll(verbResults);
                    }
                }
            }

            Iterator<Result> conjugatedFormAnalysis = results.iterator();
            StringBuilder conjugatedFormBuilder = new StringBuilder();

            while (conjugatedFormAnalysis.hasNext()) {
                Result result = conjugatedFormAnalysis.next();
                String conjugatedForm = result.surface;
                System.out.println(conjugatedForm);
                conjugatedFormBuilder.append(conjugatedForm);

                if (conjugatedFormAnalysis.hasNext()) {
                    conjugatedFormBuilder.append("\n"); // Add a separator between conjugated forms
                }
            }

            String conjugatedForm = conjugatedFormBuilder.toString();
            model.addAttribute("verb", lastVerb);
            model.addAttribute("conjugatedForm", conjugatedForm);
            model.addAttribute("times", times);
            model.addAttribute("selectedTense", tense);
            model.addAttribute("verbType", verbType);

            return "result";
        }
    }

    @PostMapping("/result")
    public String handleResultPost(@RequestParam("verb") String verb, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("verb", verb);
        return "redirect:/result";
    }
}