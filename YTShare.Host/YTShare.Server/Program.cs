using Bonjour;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Host.UseWindowsService();
builder.WebHost.UseUrls("http://0.0.0.0:7296");

// Add services to the container.

var app = builder.Build();

// Configure the HTTP request pipeline.

RegisterBonjourService();

app.MapGet("/Share", ([FromQuery] string link) =>
{
    Process.Start(new ProcessStartInfo
    {
        FileName = $"https://{link}",
        UseShellExecute = true
    });
    return $"Browser open with link:{link}";
});

app.Run();

void RegisterBonjourService()
{
    try
    {
        DNSSDService bonjourService = new DNSSDService();
        DNSSDEventManager eventManager = new DNSSDEventManager();

        eventManager.ServiceRegistered += EventManager_ServiceRegistered;

        bonjourService.Register(
            0,                      // No flags
            0,                      // Interface index (0 = all interfaces)
            "YTShareService",       // Service name
            "_http._tcp.",          // Service type (e.g., _http._tcp.)
            null,                   // Domain (null = local domain)
            null,                   // Host name (null = default)
            7296,                   // Port number
            null,                   // TXT record (null = no additional info)
            eventManager            // Event manager for callbacks
        );
    }
    catch (Exception ex)
    {
        app.Logger.LogError($"Error: {ex.Message}");
    }
}

void EventManager_ServiceRegistered(DNSSDService service, DNSSDFlags flags, string name, string regtype, string domain)
{
    app.Logger.LogInformation($"Service Registered: {name} {regtype} {domain}");
}