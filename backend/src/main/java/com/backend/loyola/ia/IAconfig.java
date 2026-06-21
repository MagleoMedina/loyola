package com.backend.loyola.ia;

public class IAconfig {

    private final OpenAI openAI;

    public IAconfig() {
        this.openAI = new OpenAI();
    }

    public String generateFeedback(String studentName, String momento, String literal) {
        String systemPrompt = """
                Eres un docente de nivel primario que redacta informes \
                pedagógicos personalizados para cada alumno. Tu tono es \
                profesional, claro y alentador. Cada informe debe ser breve \
                (máximo 3 oraciones) y estar en primera persona del plural \
                ("Hemos observado…", "Destacamos…", "Sugerimos…"). \
                No uses el nombre del estudiante en el informe.
                """;

        String userMessage = "Genera un informe para el estudiante cuyo rendimiento "
                + "durante el " + momento + " fue \"" + literal + "\". "
                + "El informe debe comenzar con \"Hemos observado que\".";

        return openAI.sendMessage(systemPrompt + "\n\n" + userMessage);
    }

    public String generateParagraph(String prompt) {
        String systemPrompt = """
                Eres un docente de nivel primario que redacta informes \
                pedagógicos detallados. Usas un tono profesional, positivo y alentador. \
                Redactas en tercera persona del singular en tiempo preterito. \
                En cada redactas informe un párrafo cohesivo que cubre todas las áreas. \
                Terminas con una frase motivacional entre comillas en otro parrafo.
                """;
        return openAI.sendMessage(systemPrompt + "\n\n" + prompt);
    }

    public String test() {
        return openAI.sendMessage("cuanto es 2 + 2");
    }
}
