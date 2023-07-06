package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.RootLexicon;

@Controller
public class ZemberekController {

    private String getRandomQueryParameter() {
        Random random = new Random();
        return "rnd=" + random.nextInt(1000000);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/result")
    public String handleResult(@RequestParam(required = false) String verb, String tense, String verbType,
            Model model) {
        if (verb == null) {

            String errorMessage = "The 'verb' parameter is required.";
            model.addAttribute("error", errorMessage);
            return "error"; // Assuming you have an "error" view to display the error message
        } else {
            if (verb.length() < 4) {
                String errorMessage = "The verb must be at least 4 characters including '-mek' or '-mak' ";
                model.addAttribute("error", errorMessage);
            }

            if (!verb.endsWith("mek") && !verb.endsWith("mak") && !verb.endsWith("mek")
                    && !verb.endsWith("mak")) {
                String errorMessage = "The 'verb' must be in infinitive form that finishes with '-mek' or '-mak'.";
                model.addAttribute("error", errorMessage);
                return "error";
            }

            String[] positiveNegatives = { "", "Neg" };
            String[] times = { "", "Aor", "Past", "Prog1", "Prog2", "Narr", "Fut" };
            String[] persons = { "A1sg", "A2sg", "A3sg", "A1pl", "A2pl", "A3pl" };
            String[] turkishPersons = { "Ben", "Sen", "O", "Biz", "Siz", "Onlar" };

            TurkishMorphology morphologyWithDefaultLexicon = TurkishMorphology.builder()
                    .setLexicon(RootLexicon.getDefault())
                    .disableCache().build();

            TurkishMorphology morphology = TurkishMorphology.builder().setLexicon(verb)
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

                        String stem = verb.substring(0, verb.length() - 3);
                        String modifiedStem = verb.substring(0, verb.length() - 4) + "d";
                        List<Result> verbResults;

                        if (stem.endsWith("t") && stem.length() > 2 && (seq.get(0) == "Prog1" || seq.get(0) == "Aor"
                                || seq.get(0) == "Fut")) {
                            verbResults = morphologyWithDefaultLexicon.getWordGenerator().generate(modifiedStem, seq);
                        } else if (stem.endsWith("r") && seq.get(0) == "Aor") {
                            TurkishMorphology morphologyWithCreateDefaults = TurkishMorphology.createWithDefaults();
                            verbResults = morphologyWithCreateDefaults.getWordGenerator().generate(stem, seq);
                        } else {
                            verbResults = morphology.getWordGenerator().generate(stem, seq);
                            System.out.println(seq);
                        }

                        if (verbResults.size() == 0) {
                            System.out.println("Cannot generate Stem = [" + stem + "] Morphemes = " + seq);
                            continue;
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

            model.addAttribute("verb", verb);

            if (conjugatedForm == null || conjugatedForm.length() < 1) {
                String errorMessage = "It is not a valid verb.";
                model.addAttribute("conjugatedForm", "");
                model.addAttribute("error", errorMessage);
                return "error";
            } else {
                model.addAttribute("conjugatedForm", conjugatedForm);
            }

            model.addAttribute("times", times);
            model.addAttribute("selectedTense", tense);
            model.addAttribute("verbType", verbType);
            model.addAttribute("persons", turkishPersons);
            return "result";
        }
    }

    @PostMapping("/result")
    public String handleResultPost(@RequestParam("verb") String verb, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("verb", verb);
        return "redirect:/result?" + getRandomQueryParameter();
    }

}