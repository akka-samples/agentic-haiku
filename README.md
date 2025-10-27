# Agentic Haiku - AI-Powered Conference Content Generation

Agentic Haiku is an intelligent system that automatically generates haiku poetry and accompanying image. The application demonstrates a workflow orchestration using AI agents for content creation and quality assurance.


## The Haiku Workflow and Its Agents

The system employs multiple specialized AI agents working together in coordinated workflows:

### Core Haiku Generation Workflow

1. **Content Quality Check** - `ToxicityDetectorAgent`
   - Analyzes input text for toxic, hateful, or inappropriate content
   - Blocks harmful content from proceeding through the pipeline

2. **Sentiment Analysis** - `SentimentDetectorAgent`
   - Evaluates the emotional tone of input messages
   - Filters out negative sentiment to maintain positive brand messaging
   - Only allows positive or neutral content to proceed

3. **Haiku Generation** - `HaikuGenAgent`
   - Creates original haiku poetry following traditional 5-7-5 syllable structure
   - Captures the essence and mood of the source material

4. **Image Generation** - Integrated with external image generation services
   - Creates visual artwork to accompany each haiku
   - Supports both Google Gemini and fixed image fallback options

## Running Locally

### Prerequisites

To use Google Gemini for AI agents and image generation, you'll need to configure Google Cloud credentials:

1. **Set up Google Cloud Project**: Ensure you have a Google Cloud project with AI Platform API enabled
2. **Configure Service Account**: Create a service account with appropriate permissions for AI Platform and Cloud Storage
3. **Set Environment Variables**:

   ```shell
   # For Google Gemini AI agents
   export GOOGLE_AI_GEMINI_API_KEY="your-gemini-api-key"
   
   # For Google Cloud services (image generation and storage)
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account-key.json"
   ```

**Note**: Without these credentials, the application will fall back to:

- Fixed/mock image generation instead of Gemini image generation
- Local file storage instead of Google Cloud Storage

### Running the Application

Start the required infrastructure (Postgres and Jaeger):

```shell
docker compose up
```

Start the service with tracing enabled:

```shell
TRACING_ENABLED=true COLLECTOR_ENDPOINT="http://localhost:4317" mvn compile exec:java
```

## Using the Application

Once the application is running (default port: 9000), you can interact with it through multiple interfaces:

### Web Interface

Access the main application through your browser: http://localhost:9000/

#### Haiku API

# generate haiku

```shell
curl -X POST http://localhost:9000/haikus/123 \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Your creative text here"
  }'
```

# get haiku result
```shell
curl http://localhost:9000/haikus/123
```