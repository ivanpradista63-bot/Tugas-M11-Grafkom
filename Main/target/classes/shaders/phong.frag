#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler; // Texture Unit 0
uniform sampler2D shadowMap;      // Texture Unit 1 (Depth)

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform int shadowBiasMode;       // 0: Dynamic, 1: Zero (Acne), 2: Large (Peter Panning)
uniform bool usePCF;              // true: 3x3 PCF, false: Single sample

float calculateShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
    // Perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    
    // Transform from NDC [-1, 1] to texture coordinate range [0, 1]
    projCoords = projCoords * 0.5 + 0.5;
    
    // If outside the far plane of the light frustum, no shadow
    if (projCoords.z > 1.0) {
        return 1.0;
    }
    
    // Determine shadow bias
    float bias;
    if (shadowBiasMode == 1) {
        bias = 0.0; // Zero bias -> Memunculkan Shadow Acne
    } else if (shadowBiasMode == 2) {
        bias = 0.06; // Bias terlalu besar -> Memunculkan Peter Panning (shadow terlepas dari objek)
    } else {
        // Dynamic slope-scale bias sesuai formulasi modul
        bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
    }
    
    // Percentage-Closer Filtering (PCF) 3x3 untuk soft shadow
    float shadowLit = 0.0;
    if (usePCF) {
        vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                shadowLit += (projCoords.z - bias > pcfDepth) ? 0.0 : 1.0;
            }
        }
        shadowLit /= 9.0;
    } else {
        float closestDepth = texture(shadowMap, projCoords.xy).r;
        shadowLit = (projCoords.z - bias > closestDepth) ? 0.0 : 1.0;
    }
    
    // Kembalikan faktor pencahayaan (0.2: bayangan, 1.0: terang benderang)
    return mix(0.2, 1.0, shadowLit);
}

void main() {
    // 1. Komponen Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;
    
    // 2. Komponen Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;
    
    // 3. Komponen Specular Lighting (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    
    if (useBlinnPhong) {
        // Blinn-Phong menggunakan Halfway Vector (H)
        vec3 halfwayDir = normalize(lightDir + viewDir);
        // Shininess eksponen 32 untuk menandingi highlight Phong eksponen 16
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        // Standard Phong menggunakan Reflection Vector (R)
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }
    vec3 specular = specularStrength * spec * lightColor;
    
    // 4. Kalkulasi Faktor Bayangan (Shadow Factor)
    float sh = useShadow ? calculateShadow(FragPosLightSpace, norm, lightDir) : 1.0;
    
    // 5. Warna Dasar Permukaan (Texture vs Solid Object Color)
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    
    // Gabungkan seluruh komponen: Ambient + Shadow * (Diffuse + Specular)
    vec3 result = (ambient + sh * (diffuse + specular)) * baseColor;
    
    FragColor = vec4(result, 1.0);
}
