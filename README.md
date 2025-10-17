# Agentic Haiku - AI-Powered Conference Content Generation

Agentic Haiku is an intelligent system that automatically generates haiku poetry and accompanying images for conference talks, with built-in content moderation and social media publishing capabilities. The application demonstrates a workflow orchestration using AI agents for content creation and quality assurance.

## What It Does

This application creates personalized haiku poems and images for conference presentations, then publishes them to social media platforms like Bluesky. It processes conference talk descriptions, extracts technical keywords, generates creative content, and manages the entire publication workflow with human approval gates.

## The Haiku Workflow and Its Agents

The system employs multiple specialized AI agents working together in coordinated workflows:

### Core Haiku Generation Workflow

1. **Content Quality Check** - `ToxicityDetectorAgent`
   - Analyzes input text for toxic, hateful, or inappropriate content
   - Blocks harmful content from proceeding through the pipeline
   - Ensures brand safety and content compliance

2. **Sentiment Analysis** - `SentimentDetectorAgent`
   - Evaluates the emotional tone of input messages
   - Filters out negative sentiment to maintain positive brand messaging
   - Only allows positive or neutral content to proceed

3. **Haiku Generation** - `HaikuGenAgent`
   - Creates original haiku poetry following traditional 5-7-5 syllable structure
   - Transforms technical conference content into artistic, engaging poems
   - Captures the essence and mood of the source material

4. **Image Generation** - Integrated with external image generation services
   - Creates visual artwork to accompany each haiku
   - Supports both Google Gemini and fixed image fallback options

### Conference-Specific Workflow

For conference talks, an additional workflow (`TalkHaikuGenerationWorkflow`) handles:

1. **Talk Analysis** - `TalkBuzzSelectorAgent`
   - Extracts 5-7 key technical terms and buzzwords from talk descriptions
   - Identifies framework names, libraries, and technical jargon
   - Focuses content generation on the most relevant technical concepts

2. **Automated Scheduling**
   - Fetches conference schedule data from external APIs
   - Calculates optimal posting times (20-50 minutes before talk start)
   - Manages speaker attribution and social media handles

### Content Management and Publishing

- **Social Post Management** - Handles approval workflows for generated content
- **Multi-Platform Publishing** - Supports Bluesky and logging-based publishers
- **Speaker Integration** - Automatically tags speakers and includes their social handles
- **Quality Gates** - Human approval required before social media publication

## Architecture Highlights

- Built on **Akka Java SDK** for distributed, resilient workflows
- **Event-driven architecture** with workflow state management
- **AI agent orchestration** for multi-step content generation
- **Observability** with Jaeger tracing and comprehensive logging

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

Access the main application through your browser:

- **Main Dashboard**: `http://localhost:9000/` - Interactive haiku generation interface with QR code scanning
- **Input Form**: `http://localhost:9000/form` - Direct text input for haiku generation
- **Social Posts Queue**: `http://localhost:9000/posts-queue` - View and manage pending social media posts
- **QR Code Scanner**: `http://localhost:9000/scan-qr-code` - Mobile-friendly QR code interface
- **Admin Audit**: `http://localhost:9000/audit` - Administrative oversight of generated content

### REST API Endpoints

#### Haiku Generation

```bash
# Get a specific haiku by ID
curl http://localhost:9000/haikus/{haikuId}

# Get real-time haiku updates (Server-Sent Events)
curl http://localhost:9000/haikus

# Monitor haiku generation progress
curl http://localhost:9000/haikus/{haikuId}/progress
```

#### Social Media Management

```bash
# List all pending social posts
curl http://localhost:9000/posts

# Approve a social media post
curl -X POST http://localhost:9000/posts/{postId}/approve

# Reject a social media post  
curl -X POST http://localhost:9000/posts/{postId}/reject

# Force publish a post (development/testing)
curl -X POST http://localhost:9000/posts/{postId}/publish
```

#### Conference Integration

```bash
# Scan conference schedule for a specific day
curl -X POST http://localhost:9000/schedule/scan/{day}

# Generate haiku for a specific talk immediately
curl -X POST http://localhost:9000/schedule/now/{proposalId}
```

#### Token Gateway (QR Code System)

```bash
# Generate a token for group participation
curl http://localhost:9000/gateway/{tokenGroupId}/token

# Submit input through token gateway
curl -X POST http://localhost:9000/gateway/inputs \
  -H "Content-Type: application/json" \
  -d '{
    "tokenGroupId": "group123",
    "token": "abc123",
    "input": "Your creative text here"
  }'
```

### Typical Workflow

1. **Start the application** as described in the running instructions
2. **Access the main interface** at `http://localhost:9000/`
3. **Generate haikus** either by:
   - Scanning QR codes with mobile devices
   - Using the direct input form at `/form`
   - Making API calls to submit text input
4. **Monitor generation** through real-time updates or API polling
5. **Manage social posts** through the posts queue interface
6. **Review and approve** content before social media publication

### Monitoring and Observability

- **Jaeger Tracing**: `http://localhost:16686` - View distributed tracing
- **Application Logs**: Monitor console output for detailed workflow information
- **Real-time Updates**: Use Server-Sent Events endpoints for live monitoring

## Features

- **Intelligent Content Filtering** - Multi-layer AI content moderation
- **Creative Content Generation** - Automated haiku and image creation  
- **Conference Integration** - Real-time schedule processing and speaker management
- **Social Media Automation** - Streamlined publishing with approval workflows
- **Observability** - Full request tracing and monitoring capabilities
