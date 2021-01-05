package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.Generation;
import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;
import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;
import nl.bioinf.cawarmerdam.compound_evolver.util.UnknownProgressException;
import nl.bioinf.cawarmerdam.compound_evolver.util.ZipHelper;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils.getProgressConnector;
import static nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils.getSessionId;

@WebServlet(name = "DownloadMultiSdfServlet", urlPatterns = "multi-sdf.download")
public class DownloadMultiSdfServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get sessions new generations
        try {
            SessionEvolutionProgressConnector progressConnector = getProgressConnector(request);
            String pipelineTargetDirectory = System.getenv("PL_TARGET_DIR");
            String sessionId = getSessionId(request);
            Path uploadDirectory = Paths.get(pipelineTargetDirectory, sessionId);

            List<Candidate> requestedCandidates = getRequestedCandidates(request, progressConnector.getGenerations());

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"conformers.zip\"");
            ArrayList<Path> list = new ArrayList<>();

            for (Candidate candidate : requestedCandidates) {
                Path resolve = uploadDirectory.resolve(String.valueOf(candidate.getIdentifier()));
                list.add(resolve);
            }

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                outputStream.write(ZipHelper.zipDirs(list));
            }

        } catch (UnknownProgressException e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    private List<Candidate> getRequestedCandidates(HttpServletRequest request, List<Generation> generations) {
        // Try to get the generation number.
        // When this fails the best conformers of the best candidates will be returned.
        try {
            int generationNumber = ServletUtils.getIntegerParameterFromRequest(request, "generationNumber");
            generations = generations.stream()
                    .filter(gen -> gen.getNumber() == generationNumber).collect(Collectors.toList());
        } catch (ServletUtils.FormFieldHandlingException ignored) {
        }
        return getRequestedCandidatesFromGenerations(request, generations);
    }

    private List<Candidate> getRequestedCandidatesFromGenerations(HttpServletRequest request, List<Generation> generations) {
        if (ServletUtils.getBooleanParameterFromRequest(request, "bestOnly")) {
            return generations.stream()
                    .map(Generation::getFittestCandidate).collect(Collectors.toList());
        } else {
            return generations.stream()
                    .flatMap(gen -> gen.getCandidateList().stream())
                    .collect(Collectors.toList());
        }
    }

}
